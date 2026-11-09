package com.smarttraveler.dummy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** Capacity shown in a TourAPI room title, with explicit detailInfo2 counts taking precedence. */
final class RoomCapacity {
    private static final Pattern RANGE = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*[~\\-–]\\s*(\\d{1,2})\\s*인");
    private static final Pattern PEOPLE = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*인(?:실)?");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,2}");

    record Spec(String name, int base, int max) {}

    static Spec from(JsonNode room, String fallbackName, int fallbackBase, int fallbackMax) {
        String title = room == null ? "" : room.path("name").asString("").trim();
        if (title.isBlank()) title = fallbackName;
        int base = fallbackBase;
        int max = fallbackMax;
        Matcher range = RANGE.matcher(title);
        Matcher people = PEOPLE.matcher(title);
        boolean named = false;
        if (range.find()) {
            base = Integer.parseInt(range.group(1));
            max = Integer.parseInt(range.group(2));
            named = true;
        } else if (people.find()) {
            max = Integer.parseInt(people.group(1));
            base = Math.min(2, max);
            named = true;
        }
        int explicitBase = count(room, "base_capacity");
        int explicitMax = count(room, "max_capacity");
        if (explicitBase > 0) base = explicitBase;
        if (explicitMax > 0) max = explicitMax;
        else if (explicitBase > 0) max = Math.max(max, explicitBase);
        max = Math.max(1, max);
        base = Math.max(1, Math.min(base, max));
        boolean nameMatches = !named;
        // A title without a capacity needs a visible maximum. Conflicting API counts also
        // replace the number in the title so the label never advertises the old capacity.
        if (named) {
            Matcher parsedRange = RANGE.matcher(title);
            Matcher parsedPeople = PEOPLE.matcher(title);
            if (parsedRange.find()) {
                nameMatches = base == Integer.parseInt(parsedRange.group(1))
                        && max == Integer.parseInt(parsedRange.group(2));
            } else if (parsedPeople.find()) {
                nameMatches = max == Integer.parseInt(parsedPeople.group(1));
            }
        }
        if (!named || !nameMatches) {
            if (named) {
                Matcher standardCount = PEOPLE.matcher(title);
                boolean namedBase = !RANGE.matcher(title).find() && standardCount.find()
                        && base == Integer.parseInt(standardCount.group(1)) && max > base;
                if (!namedBase) title = PEOPLE.matcher(RANGE.matcher(title).replaceAll("")).replaceAll("").trim();
            }
            if (title.isBlank()) title = "객실";
            title += " (최대 " + max + "인)";
        }
        return new Spec(title, base, max);
    }

    private static int count(JsonNode room, String field) {
        if (room == null) return 0;
        Matcher matcher = NUMBER.matcher(room.path(field).asString(""));
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private RoomCapacity() {}
}
