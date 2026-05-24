#!/usr/bin/env bash
# Tests for the bash logic in .github/workflows/generate-tests.yml

PASS=0
FAIL=0

assert_eq() {
    local description="$1"
    local expected="$2"
    local actual="$3"

    if [ "$expected" = "$actual" ]; then
        echo "  PASS: $description"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $description"
        echo "        expected: $(echo "$expected" | cat -A)"
        echo "        actual:   $(echo "$actual" | cat -A)"
        FAIL=$((FAIL + 1))
    fi
}

# ---------------------------------------------------------------------------
# extract_issue_code: mirrors the grep logic in the "Commit generated tests" step
# ---------------------------------------------------------------------------
extract_issue_code() {
    local branch="$1"
    local code
    code=$(echo "$branch" | grep -oE '[A-Z]+-[a-z0-9]+' | head -1)
    echo "${code:-NO-ISSUE}"
}

echo "--- extract_issue_code ---"
assert_eq "standard feature branch"       "TAPIEMS-943" "$(extract_issue_code "feature/TAPIEMS-943-some-description")"
assert_eq "NJP prefix with ClickUp alphanumeric suffix" "NJP-868jdc4jv" "$(extract_issue_code "feature/NJP-868jdc4jv-project-init")"
assert_eq "hotfix branch"                 "ABC-123"     "$(extract_issue_code "hotfix/ABC-123-fix-something")"
assert_eq "multi-token branch picks first" "FOO-1"      "$(extract_issue_code "feature/FOO-1-BAR-2-desc")"
assert_eq "no issue code → NO-ISSUE"      "NO-ISSUE"    "$(extract_issue_code "feature/no-issue-here")"
assert_eq "main branch → NO-ISSUE"        "NO-ISSUE"    "$(extract_issue_code "main")"
assert_eq "bare numeric suffix"           "XY-99"       "$(extract_issue_code "refs/heads/XY-99-desc")"
assert_eq "lowercase letters not matched" "NO-ISSUE"    "$(extract_issue_code "feature/abc-123-lowercase")"

# ---------------------------------------------------------------------------
# build_commit_body: mirrors the while-loop in the "Commit generated tests" step
# ---------------------------------------------------------------------------
build_commit_body() {
    local issue_code="$1"
    local staged_files="$2"

    local commit_body=""
    local last_file
    last_file=$(echo "$staged_files" | tail -1)

    while IFS= read -r file; do
        if [ "$file" = "$last_file" ]; then
            commit_body="${commit_body}- ${issue_code}: Generated/updated test file \`${file}\`"
        else
            commit_body="${commit_body}- ${issue_code}: Generated/updated test file \`${file}\`;\n"
        fi
    done <<< "$staged_files"

    printf "%b" "$commit_body"
}

echo ""
echo "--- build_commit_body ---"

single=$(build_commit_body "NJP-1" "src/test/Foo.java")
assert_eq "single file: no trailing semicolon" \
    "- NJP-1: Generated/updated test file \`src/test/Foo.java\`" \
    "$single"

two=$(build_commit_body "NJP-2" "$(printf 'src/test/A.java\nsrc/test/B.java')")
first_line=$(echo "$two" | head -1)
last_line=$(echo "$two" | tail -1)
assert_eq "two files: first line ends with semicolon" \
    "- NJP-2: Generated/updated test file \`src/test/A.java\`;" \
    "$first_line"
assert_eq "two files: last line has no semicolon" \
    "- NJP-2: Generated/updated test file \`src/test/B.java\`" \
    "$last_line"

three=$(build_commit_body "NJP-3" "$(printf 'a.java\nb.java\nc.java')")
mid_line=$(echo "$three" | sed -n '2p')
last_three=$(echo "$three" | tail -1)
assert_eq "three files: middle line ends with semicolon" \
    "- NJP-3: Generated/updated test file \`b.java\`;" \
    "$mid_line"
assert_eq "three files: last line has no semicolon" \
    "- NJP-3: Generated/updated test file \`c.java\`" \
    "$last_three"

# ---------------------------------------------------------------------------
# build_commit_title: mirrors COMMIT_TITLE assignment
# ---------------------------------------------------------------------------
build_commit_title() {
    local issue_code="$1"
    echo "${issue_code}: Generated tests by Claude Code agent"
}

echo ""
echo "--- build_commit_title ---"
assert_eq "title contains issue code" \
    "NJP-42: Generated tests by Claude Code agent" \
    "$(build_commit_title "NJP-42")"
assert_eq "title with NO-ISSUE fallback" \
    "NO-ISSUE: Generated tests by Claude Code agent" \
    "$(build_commit_title "NO-ISSUE")"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
