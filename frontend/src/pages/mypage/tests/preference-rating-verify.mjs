import assert from 'node:assert/strict';
import { ratingSelection, selectionRating } from '../data/preferenceRating.js';
import { CHECKBOX_TASTES, preferenceError, tagKey } from '../data/settings.js';

const options = Object.values(CHECKBOX_TASTES).flat();
assert.equal(new Set(options.map(tagKey)).size, options.length);
assert.equal(selectionRating(undefined), null);
const representations = new Set();
for (let rating = 1; rating <= 5; rating++) {
  const tags = options.map(option => ratingSelection(option, rating));
  assert.equal(preferenceError({ pace: 'normal', wakeUp: 'normal', tags }, options), '');
  for (const tag of tags) assert.equal(selectionRating(JSON.parse(JSON.stringify(tag))), rating);
  representations.add(JSON.stringify([tags[0].polarity, tags[0].weight]));
}
assert.equal(representations.size, 5);
for (const invalid of [0, 6, 1.5, null]) assert.throws(() => ratingSelection(options[0], invalid));
const defaults = options.map(option => ratingSelection(option, 1));
assert.ok(defaults.every(tag => selectionRating(tag) === 1));
const cleared = defaults.filter(tag => tagKey(tag) !== tagKey(options[0]));
assert.equal(selectionRating(cleared.find(tag => tagKey(tag) === tagKey(options[0]))), null);
assert.equal(preferenceError({ pace: 'normal', wakeUp: 'normal', tags: cleared }, options), '');
console.log('PASS: five distinct rating values, all option validation, default dislike, unset persistence, invalid step rejection');
