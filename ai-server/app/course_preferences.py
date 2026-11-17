"""Deterministic, conservative preference parsing. Raw prose stays in the query."""
import json
import re

ALIASES = {
    "pace": {"relaxed": ("relaxed", "여유", "느긋", "휴식파"),
             "normal": ("normal", "보통", "적당"), "packed": ("packed", "알찬", "빡빡", "촘촘")},
    "environment": {"indoor": ("indoor", "실내"), "outdoor": ("outdoor", "야외")},
    "landscapes": {"city": ("city", "도시"), "countryside": ("countryside", "시골", "농촌"),
                   "water": ("water", "호수·강", "호수", "강변"), "island": ("island", "섬"),
                   "forest": ("forest", "산·숲", "산", "숲"), "sea": ("sea", "바다", "해변", "바다·해변")},
}
NEGATIVE = r"(?:제외|피하|피해|싫|말고|말아|안\s*가|가지\s*말|원하지\s*않|선호하지\s*않|않|없|아닌)"


def matches(text, alias):
    # Single syllables must be standalone tokens (산책 is not 산).
    if len(alias) == 1:
        return re.search(r"(?<![가-힣a-z])" + alias + r"(?=$|[\s·,:]|은|는|을|를|이|가|에|도)", text) is not None
    return alias in text


def normalized_landscapes(value):
    if isinstance(value, str):
        try:
            value = json.loads(value)
        except (ValueError, TypeError):
            value = [value]
    if not isinstance(value, list):
        return []
    return sorted({key for key, aliases in ALIASES["landscapes"].items()
                   if any(item in aliases for item in value if isinstance(item, str))})


def effective_preferences(conditions, saved=None):
    saved = saved or {}
    effective = {}
    for field in ("pace", "environment"):
        if saved.get(field) in ALIASES[field]:
            effective[field] = saved[field]
    landscapes = normalized_landscapes(saved.get("landscapes", []))
    if landscapes:
        effective["landscapes"] = landscapes
    excluded = {field: set() for field in ALIASES}
    explicit = {field: set() for field in ALIASES}
    unknown_exclusion = False
    environment_any = False
    for raw in conditions.get("preferences") or []:
        for phrase in re.split(r"[,;\n]|(?:지만|대신)|(?:하고|그리고)\s", raw.lower()):
            negative = bool(re.search(NEGATIVE, phrase))
            found = False
            both = "둘 다" in phrase or "둘다" in phrase or "실내외" in phrase
            if both:
                environment_any = True
                found = True
            occurrences = []
            for field, options in ALIASES.items():
                if field == "environment" and both:
                    continue
                for key, aliases in options.items():
                    for alias in aliases:
                        if matches(phrase, alias):
                            occurrences.append((phrase.index(alias), len(alias), field, key))
            # Prefer longest overlapping aliases and determine polarity locally,
            # so "야외 제외 실내 위주" does not exclude both environments.
            tokens = []
            for token in sorted(occurrences, key=lambda x: (x[0], -x[1])):
                if not tokens or token[0] >= tokens[-1][0] + tokens[-1][1]:
                    tokens.append(token)
            for i, (start, length, field, key) in enumerate(tokens):
                end = tokens[i + 1][0] if i + 1 < len(tokens) else len(phrase)
                local_negative = bool(re.search(NEGATIVE, phrase[start + length:end]))
                (excluded if local_negative else explicit)[field].add(key)
                found = True
            if negative and not found:
                unknown_exclusion = True
    for field in ("pace", "environment"):
        if explicit[field]:
            effective.pop(field, None)
            if len(explicit[field]) == 1:
                effective[field] = next(iter(explicit[field]))
        if effective.get(field) in excluded[field]:
            effective.pop(field, None)
    if environment_any and not explicit["environment"]:
        effective.pop("environment", None)
    if explicit["landscapes"]:
        effective["landscapes"] = sorted(explicit["landscapes"])
    if "landscapes" in effective:
        effective["landscapes"] = sorted(set(effective["landscapes"]) - excluded["landscapes"])
        if not effective["landscapes"]:
            effective.pop("landscapes")
    return effective, excluded, unknown_exclusion


def satisfies_exclusions(info, excluded):
    for field in ("pace", "environment"):
        if excluded[field] and (info.get(field) not in ALIASES[field] or info[field] in excluded[field]):
            return False
    landscapes = normalized_landscapes(info.get("landscapes"))
    if excluded["landscapes"] and (not landscapes or set(landscapes) & excluded["landscapes"]):
        return False
    return True


def preference_score(info, relevance, effective):
    score, weight = .5 * relevance, .5
    matched = []
    for field, factor in (("pace", .2), ("environment", .15)):
        if field in effective:
            value = info.get(field)
            agreement = .5 if value not in ALIASES[field] else float(value == effective[field])
            score += factor * agreement
            weight += factor
            if agreement == 1:
                matched.append(effective[field])
    if effective.get("landscapes"):
        actual = set(normalized_landscapes(info.get("landscapes")))
        wanted = set(effective["landscapes"])
        overlap = actual & wanted
        score += .15 * (len(overlap) / len(wanted) if actual else .5)
        weight += .15
        matched.extend(sorted(overlap))
    return score / weight, matched
