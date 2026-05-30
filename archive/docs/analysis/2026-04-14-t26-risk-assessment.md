# T26 Risk Assessment

## Risk 1
Refactoring mapper metadata could change binding names accidentally.
- Mitigation: keep method names/signatures stable and run backend tests.

## Risk 2
Over-refactoring mapper style could expand scope.
- Mitigation: only converge obvious duplication; do not rewrite every mapper.

## Risk 3
XML fragment extraction could introduce alias mistakes.
- Mitigation: use separate SQL fragments for aliased and non-aliased lifecycle-log columns.
