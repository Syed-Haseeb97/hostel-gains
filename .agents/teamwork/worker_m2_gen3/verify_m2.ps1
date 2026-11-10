$m1_m2_files = @(
    'TEST-com.hostelgains.data.nutrition.ChallengerM2AdversarialStressTest.xml',
    'TEST-com.hostelgains.data.nutrition.DailyNutritionAggregationTest.xml',
    'TEST-com.hostelgains.data.nutrition.NutritionParserTest.xml',
    'TEST-com.hostelgains.domain.challenger.M1AdversarialStressTest.xml',
    'TEST-com.hostelgains.domain.challenger.M2AdversarialStressTest.xml',
    'TEST-com.hostelgains.domain.ChallengerM1AdversarialStressTest.xml',
    'TEST-com.hostelgains.domain.CompensatoryEngineTest.xml',
    'TEST-com.hostelgains.domain.StepCounterSyncTest.xml',
    'TEST-com.hostelgains.domain.VTaperFrameworkStructureTest.xml',
    'TEST-com.hostelgains.e2e.Tier1FeatureCoverageTest.xml',
    'TEST-com.hostelgains.e2e.Tier2BoundaryCornerCaseTest.xml',
    'TEST-com.hostelgains.e2e.Tier3CombinationsTest.xml',
    'TEST-com.hostelgains.e2e.Tier4HostelScenariosTest.xml'
)

$totalTests = 0
$totalFailures = 0
$totalErrors = 0
$results = @()

foreach ($file in $m1_m2_files) {
    $path = Join-Path 'D:\projects\hostel_gains\app\build\test-results\testDebugUnitTest' $file
    if (Test-Path $path) {
        [xml]$x = Get-Content $path
        $tests = [int]$x.testsuite.tests
        $failures = [int]$x.testsuite.failures
        $errors = [int]$x.testsuite.errors
        $totalTests += $tests
        $totalFailures += $failures
        $totalErrors += $errors
        $results += [PSCustomObject]@{ File = $file; Tests = $tests; Failures = $failures; Errors = $errors }
    } else {
        Write-Warning "Missing $file"
    }
}

$results | Format-Table -AutoSize
Write-Host "Total M1/M2 Tests: $totalTests, Failures: $totalFailures, Errors: $totalErrors"
