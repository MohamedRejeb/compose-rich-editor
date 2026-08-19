// Raise the per-test timeout for browser test targets. The seeded fuzz suites
// (RichTextEditCorruptionFuzzTest, RichTextHtmlRoundTripFuzzTest, Issue716StringIndexFuzzTest,
// RichTextJsonRoundTripFuzzTest) run hundreds of scenarios inside a single test method and
// exceed mocha's 2s default on slow CI runners, failing with a bare "Error" and no message.
config.set({
    client: {
        mocha: {
            timeout: 120000
        }
    }
});
