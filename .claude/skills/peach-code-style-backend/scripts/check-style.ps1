# Peach Backend Code Style Checker (PowerShell)
# Usage: .\check-style.ps1 [-Target <path>]
# Exit: 0=pass, 1=fail

param(
    [string]$Target = "."
)

$Errors = 0
$Warnings = 0

function Write-Error-Line($msg) {
    Write-Host "[ERROR] $msg" -ForegroundColor Red
    $script:Errors++
}

function Write-Warn-Line($msg) {
    Write-Host "[WARN]  $msg" -ForegroundColor Yellow
    $script:Warnings++
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Peach Backend Code Style Checker" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$javaFiles = Get-ChildItem -Path $Target -Recurse -Filter "*.java" -File -ErrorAction SilentlyContinue
$xmlFiles  = Get-ChildItem -Path $Target -Recurse -Filter "*.xml" -File -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match "mapper" }

# 1. Java 9+ syntax
Write-Host ">> [1/9] Checking Java 9+ prohibited syntax..."
foreach ($f in $javaFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match '^\s+var\s+\w+\s*=') {
        Write-Error-Line "$($f.FullName): Uses var (Java 10+, prohibited)"
    }
    if ($content -match '\b(List|Map|Set)\.(of|copyOf)\(') {
        Write-Error-Line "$($f.FullName): Uses List.of/Map.of (Java 9+, prohibited)"
    }
    if ($content -match '"""') {
        Write-Error-Line "$($f.FullName): Uses text block (Java 13+, prohibited)"
    }
    if ($content -match 'public\s+record\s+') {
        Write-Error-Line "$($f.FullName): Uses record (Java 14+, prohibited)"
    }
}

# 2. System.out
Write-Host ">> [2/9] Checking System.out.println..."
foreach ($f in $javaFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match 'System\.(out|err)\.(print|println)') {
        Write-Error-Line "$($f.FullName): Uses System.out/err (use @Slf4j instead)"
    }
}

# 3. Controller annotation
Write-Host ">> [3/9] Checking Controller annotation..."
$controllerFiles = $javaFiles | Where-Object { $_.Name -like "*Controller.java" }
foreach ($f in $controllerFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match '@RestController') {
        if ($content -notmatch '@Tag') {
            Write-Error-Line "$($f.FullName): Controller missing @Tag"
        }
        if ($content -notmatch '@Slf4j') {
            Write-Warn-Line "$($f.FullName): Controller missing @Slf4j"
        }
        if ($content -notmatch '@Operation') {
            Write-Error-Line "$($f.FullName): Controller method missing @Operation"
        }
    }
}

# 4. Service annotation
Write-Host ">> [4/9] Checking Service annotation..."
$serviceFiles = $javaFiles | Where-Object { $_.Name -like "*ServiceImpl.java" }
foreach ($f in $serviceFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match '@Service') {
        if ($content -notmatch '@Slf4j') {
            Write-Warn-Line "$($f.FullName): Missing @Slf4j"
        }
        if ($content -notmatch '@Indexed') {
            Write-Warn-Line "$($f.FullName): Missing @Indexed"
        }
    }
}

# 5. DAO annotation
Write-Host ">> [5/9] Checking DAO annotation..."
$daoFiles = $javaFiles | Where-Object { $_.Name -like "*Dao.java" }
foreach ($f in $daoFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match 'interface') {
        if ($content -notmatch '@MybatisDao') {
            Write-Error-Line "$($f.FullName): DAO missing @MybatisDao"
        }
        if ($content -notmatch 'PeachDao') {
            Write-Warn-Line "$($f.FullName): DAO does not extend PeachDao"
        }
    }
}

# 6. Naming convention
Write-Host ">> [6/9] Checking naming convention..."
foreach ($f in $javaFiles) {
    $name = $f.BaseName
    if ($f.FullName -match "[\\/]entity[\\/]" -and $name -notmatch "(DO|Base|Peach)$" -and $name -notmatch "^Base") {
        Write-Warn-Line "$($f.FullName): Entity class should end with DO"
    }
}

# 7. MyBatis XML
Write-Host ">> [7/9] Checking MyBatis XML..."
foreach ($f in $xmlFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match 'SELECT\s+\*\s+FROM') {
        Write-Error-Line "$($f.FullName): Uses SELECT * (prohibited)"
    }
    if ($content -match 'namespace\s*=\s*""') {
        Write-Error-Line "$($f.FullName): namespace is empty"
    }
    foreach ($frag in @("allColumn", "allColumnAlias", "allColumnValue", "allColumnCond")) {
        if ($content -notmatch "id=`"$frag`"") {
            Write-Warn-Line "$($f.FullName): Missing SQL fragment $frag"
        }
    }
}

# 8. Serializable
Write-Host ">> [8/12] Checking Serializable..."
$dataObjFiles = $javaFiles | Where-Object { $_.Name -match "(DO|DTO|QO|VO)\.java$" }
foreach ($f in $dataObjFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -notmatch 'Serializable') {
        Write-Error-Line "$($f.FullName): Does not implement Serializable"
    }
    if ($content -notmatch 'serialVersionUID') {
        Write-Warn-Line "$($f.FullName): Missing serialVersionUID"
    }
}

# 9. Controller method naming and @UserOperLog
Write-Host ">> [9/12] Checking Controller method naming and @UserOperLog..."
foreach ($f in $controllerFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match '@RestController') {
        # Check for save/modify/delete methods
        if ($content -match 'public\s+Response\s+(save|modify|delete)\w+\(') {
            # Check if @UserOperLog exists
            if ($content -notmatch '@UserOperLog') {
                Write-Error-Line "$($f.FullName): Controller non-query method missing @UserOperLog"
            }
        }
    }
}

# 10. Parameter validation check
Write-Host ">> [10/12] Checking parameter validation..."
foreach ($f in $controllerFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    # Check if Controller uses @Validated
    if ($content -match '@Validated') {
        # Extract parameter types (QO/DTO)
        $paramMatches = [regex]::Matches($content, '@Validated\([^)]+\)\s+@RequestBody\s+(\w+)\s+(\w+)')
        foreach ($match in $paramMatches) {
            $paramType = $match.Groups[1].Value
            if ($paramType -match '(QO|DTO)$') {
                # Find corresponding QO/DTO file
                $qoDtoFile = $javaFiles | Where-Object { $_.Name -eq "$paramType.java" } | Select-Object -First 1
                if ($qoDtoFile) {
                    $qoDtoContent = Get-Content $qoDtoFile.FullName -Raw -ErrorAction SilentlyContinue
                    # Check if the file has validation annotations
                    if ($qoDtoContent -notmatch '@NotNull|@NotBlank|@NotEmpty|@Size|@Pattern|@Min|@Max|@DecimalMin|@DecimalMax') {
                        Write-Warn-Line "$($f.FullName): Controller uses @Validated but parameter $paramType has no validation rules"
                    }
                }
            }
        }
    }
}

# 11. Primary key and time field check
Write-Host ">> [11/12] Checking primary key and time field..."
$doFiles = $javaFiles | Where-Object { $_.Name -match "DO\.java$" -and $_.Name -ne "PeachDO.java" }
foreach ($f in $doFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    # Check for auto-increment primary key
    if ($content -match '@GeneratedValue.*IDENTITY') {
        Write-Error-Line "$($f.FullName): DO primary key uses auto-increment (prohibited)"
    }
    # Check for Long/Integer primary key
    if ($content -match '@Id') {
        if ($content -match 'private\s+(Long|Integer)\s+id') {
            Write-Error-Line "$($f.FullName): DO primary key must be String (UUID), not Long/Integer"
        }
    }
    # Check for Date/LocalDateTime time field
    if ($content -match 'private\s+(Date|LocalDateTime|Timestamp)\s+') {
        Write-Error-Line "$($f.FullName): Time field must be String, not Date/LocalDateTime/Timestamp"
    }
    # Check for DO overriding parent fields (prohibited)
    if ($content -match '@Override' -and $content -match 'getCreateTime|getModifyTime|getCreateUserCode|getCreateUserName|getUpdateUserCode|getUpdateUserName|getCreateUserId|getModifyUserId') {
        Write-Error-Line "$($f.FullName): DO should not override parent PeachDO fields (use inherited fields directly)"
    }
}

# 12. @UserOperLog moduleCode must use enum
Write-Host ">> [12/12] Checking @UserOperLog moduleCode uses enum..."
foreach ($f in $controllerFiles) {
    $content = Get-Content $f.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match '@UserOperLog') {
        # Check if moduleCode is a string literal (bad) instead of enum (good)
        if ($content -match 'moduleCode\s*=\s*"[^"]+"') {
            Write-Error-Line "$($f.FullName): @UserOperLog moduleCode must use enum (UserLogEnum.Module.XXX), not string literal"
        }
    }
}

# Result
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Check Complete" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if ($Errors -gt 0) {
    Write-Host " Errors:   $Errors" -ForegroundColor Red
} else {
    Write-Host " Errors:   $Errors" -ForegroundColor Green
}

if ($Warnings -gt 0) {
    Write-Host " Warnings: $Warnings" -ForegroundColor Yellow
} else {
    Write-Host " Warnings: $Warnings" -ForegroundColor Green
}

Write-Host ""

if ($Errors -gt 0) {
    Write-Host "Check failed with $Errors error(s)" -ForegroundColor Red
    exit 1
} elseif ($Warnings -gt 0) {
    Write-Host "Check passed with $Warnings warning(s)" -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "All checks passed" -ForegroundColor Green
    exit 0
}