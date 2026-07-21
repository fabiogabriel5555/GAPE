# Assessment Tests

## Scope

The assessment service tests cover Phase 11 back-end rules for questionnaires, exams, questions, options, attempts, responses and correction.

## Automated Coverage

- `AssessmentServiceTest`
  - Valid online and onsite questionnaires.
  - Valid online and onsite exams.
  - Subject-level exams persist explicit applicable class groups.
  - Subject-level exams without applicable class groups are rejected.
  - Teachers cannot create subject-level exams for unmanaged class groups.
  - Assessment final-grade weight is required and must stay between 0 and 100.
  - Passing grade cannot exceed maximum grade.
  - Attempts limit must be greater than zero when defined.
  - Questionnaires require a content block.
  - Structural assessment changes, including attempt-limit changes, are blocked after attempts have started.
  - Automatic correction mode rejects existing manual active questions.

- `QuestionServiceTest`
  - Valid question creation.
  - Questions require an existing assessment.
  - Teachers cannot create questions for unmanaged assessments.
  - Question scores cannot be negative.
  - Active question score totals cannot exceed the assessment maximum grade.
  - Active question scores can be rebalanced to the assessment maximum grade.
  - A corrected attempt reaches the assessment maximum when every response is
    awarded its question maximum; the response total and attempt total must
    remain equal.
  - Automatic assessments reject manual-scored question types.
  - Submitted attempts block question changes.

- `QuestionOptionServiceTest`
  - Valid option creation.
  - Options require an existing question.
  - Options are rejected for question types that do not admit options.
  - Single-choice questions cannot have two active correct options.
  - Automatic multiple-choice questions require at least one active correct option.
  - Submitted attempts block option changes.

- `AttemptServiceTest`
  - Student can start an attempt inside the availability window.
  - Attempts before the start, at the exact end and outside the availability window are rejected.
  - In-person assessments cannot be started online.
  - Assessments without active questions cannot be started.
  - Attempts above the configured limit are rejected.
  - Non-students and inactive students cannot start attempts.
  - Submission requires required responses.

- `ResponseServiceTest`
  - Student can save a single-choice response.
  - Option questions reject text answers.
  - Responses must target questions from the attempt assessment.
  - Responses for inactive questions are rejected.
  - Single-choice responses reject multiple selected options.
  - Paragraph and file-upload responses are valid for compatible question types.
  - Hidden attachment forgery without an existing response is rejected.
  - Unsafe attachment paths are rejected.
  - Responses after the availability window closes are rejected.

- `CorrectionServiceTest`
  - Automatic correction scores objective responses.
  - Mixed correction leaves manual responses pending while scoring objective responses.
  - Manual correction applies a valid score and corrects the attempt.
  - Manual correction cannot exceed the question score.
  - Automatic correction rejects manual-only assessments.
  - Manual correction rejects automatic-only assessments.
  - Correction before submission is rejected.
  - Negative manual scores are rejected.
  - Students cannot correct attempts.

- `ContentItemServiceTest`
  - Reusable file-backed repository content can be listed, selected and previewed by non-student actors without original or target-context permission.
  - Students cannot use reusable repository APIs and still need normal content access for preview/download.

- `SchemaIntegrityTest`
  - Assessment mode checks and assessment/attempt/response validation triggers are present.

- `DatabaseBootstrapServiceTest`
  - `Functions Applied Checkpoint` has ten required questions whose scores sum
    to 20.00.
  - Its corrected fixture includes one full-score 20.00 attempt, two corrected
    attempts in total and three submitted attempts awaiting correction.

- `TemplateStructureTest`
  - Legacy quiz-attempt wrappers redirect to the real assessment flow without requiring stale sidebar state.

## Commands

```bash
mvn test -Dtest=AssessmentServiceTest
mvn test -Dtest=QuestionServiceTest
mvn test -Dtest=QuestionOptionServiceTest
mvn test -Dtest=AttemptServiceTest
mvn test -Dtest=ResponseServiceTest
mvn test -Dtest=CorrectionServiceTest
mvn test -Dtest=ContentItemServiceTest
mvn test -Dtest=SchemaIntegrityTest
mvn test -Dtest=DatabaseRestrictionCoverageTest
mvn test -Dtest=TemplateStructureTest
mvn -q "-Dtest=ContentItemServiceTest,AssessmentServiceTest,QuestionServiceTest,QuestionOptionServiceTest,AttemptServiceTest,ResponseServiceTest,CorrectionServiceTest,SchemaIntegrityTest,DatabaseRestrictionCoverageTest,TemplateStructureTest" test
mvn -q test
```

All listed commands were executed successfully after the correction pass.
