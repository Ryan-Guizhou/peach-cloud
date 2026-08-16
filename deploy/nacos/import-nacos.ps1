#Requires -Version 5.1

# ============================================================
# Nacos configuration batch import script
# Compatible with Windows PowerShell 5.1 and PowerShell 7+
# Supports Nacos 1.x/2.x and Nacos 3.x
# ============================================================

# ================= User settings =================
$NACOS_SERVER = "http://127.0.0.1:8848"
$NACOS_USERNAME = "nacos"
$NACOS_PASSWORD = "nacos"

# IMPORTANT: this must be the Namespace ID, not only the display name.
$NAMESPACE = "d1e5878e-06dd-44e9-8832-6d0e7c9a312e"
$GROUP = "PEACH-CLOUD"

# Relative to this .ps1 file
$CONFIG_DIR = ".\config"

# auto / 2 / 3
$NACOS_API_MODE = "auto"

$ENABLE_AUTH = $true
$VERIFY_AFTER_IMPORT = $true
$VERIFY_RETRY_COUNT = 20
$VERIFY_RETRY_INTERVAL_MS = 500
$CHECK_NAMESPACE = $true
$VALIDATE_JSON = $true
$REQUEST_TIMEOUT_SECONDS = 15
$PAUSE_ON_EXIT = $true

# Private values are read from this JSON file before import. It is never
# written by this script and must not be placed under $CONFIG_DIR.
$IMPORT_VALUES_FILE = ".\import-nacos.private.json"
# =================================================

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    $BASE_DIR = (Get-Location).Path
}
else {
    $BASE_DIR = $PSScriptRoot
}

function Write-Info {
    param([string]$Message)
    Write-Host ("[INFO] " + $Message) -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host ("[OK]   " + $Message) -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host ("[WARN] " + $Message) -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Message)
    Write-Host ("[ERR]  " + $Message) -ForegroundColor Red
}

function Resolve-ScriptPath {
    param([string]$Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }

    return [System.IO.Path]::GetFullPath((Join-Path $BASE_DIR $Path))
}

function Get-NacosUrl {
    param([string]$Path)

    $server = $NACOS_SERVER.TrimEnd("/")

    if ($server.EndsWith("/nacos", [System.StringComparison]::OrdinalIgnoreCase)) {
        return ($server + $Path)
    }

    return ($server + "/nacos" + $Path)
}

function Encode-UrlValue {
    param([AllowEmptyString()][string]$Value)
    return [System.Uri]::EscapeDataString([string]$Value)
}

function Get-LegacyTenant {
    if ([string]::IsNullOrWhiteSpace($NAMESPACE)) {
        return ""
    }

    if ($NAMESPACE -ieq "public") {
        return ""
    }

    return $NAMESPACE
}

function Get-HttpError {
    param($ErrorRecord)

    $statusCode = $null
    $detail = $null

    try {
        if ($null -ne $ErrorRecord.Exception.Response) {
            $statusCode = [int]$ErrorRecord.Exception.Response.StatusCode
        }
    }
    catch {
        $statusCode = $null
    }

    try {
        if ($null -ne $ErrorRecord.ErrorDetails) {
            $detail = $ErrorRecord.ErrorDetails.Message
        }
    }
    catch {
        $detail = $null
    }

    if ([string]::IsNullOrWhiteSpace($detail)) {
        $detail = $ErrorRecord.Exception.Message
    }

    return [PSCustomObject]@{
        StatusCode = $statusCode
        Detail = $detail
    }
}

function Get-NacosApiMode {
    $mode = ([string]$NACOS_API_MODE).Trim().ToLowerInvariant()

    if ($mode -eq "2") {
        return "2"
    }

    if ($mode -eq "3") {
        return "3"
    }

    if ($mode -ne "auto") {
        throw "NACOS_API_MODE must be auto, 2 or 3."
    }

    Write-Info "Detecting Nacos API version..."

    $healthUrl = Get-NacosUrl "/v3/admin/core/state/liveness"

    try {
        $response = Invoke-RestMethod `
            -Uri $healthUrl `
            -Method Get `
            -TimeoutSec 5 `
            -ErrorAction Stop

        if (($response.code -eq 0) -and ([string]$response.data -eq "ok")) {
            Write-Ok "Detected Nacos 3.x."
            return "3"
        }
    }
    catch {
        # Nacos 1.x/2.x normally does not have this endpoint.
    }

    Write-Info "Nacos 3.x health API not detected. Using Nacos 1.x/2.x API."
    return "2"
}


function Test-NacosNamespace {
    param(
        [string]$ApiMode,
        [AllowNull()][string]$Token
    )

    if (-not $CHECK_NAMESPACE) {
        return $true
    }

    if ($ApiMode -ne "2") {
        return $true
    }

    # public namespace is represented by empty tenant in Nacos 1.x/2.x API.
    if ([string]::IsNullOrWhiteSpace($NAMESPACE) -or ($NAMESPACE -ieq "public")) {
        Write-Ok "Using public namespace."
        return $true
    }

    $baseUrl = Get-NacosUrl "/v1/console/namespaces"
    $url = $baseUrl

    if ($ENABLE_AUTH -and -not [string]::IsNullOrWhiteSpace($Token)) {
        $encodedToken = Encode-UrlValue $Token
        $url = "{0}?accessToken={1}" -f $baseUrl, $encodedToken
    }

    Write-Info ("Checking namespace ID: " + $NAMESPACE)

    try {
        $response = Invoke-RestMethod `
            -Uri $url `
            -Method Get `
            -TimeoutSec $REQUEST_TIMEOUT_SECONDS `
            -ErrorAction Stop

        $items = @($response.data)

        $idMatch = @(
            $items | Where-Object {
                [string]$_.namespace -ceq [string]$NAMESPACE
            }
        )

        if ($idMatch.Count -gt 0) {
            Write-Ok ("Namespace ID exists: " + $NAMESPACE)
            return $true
        }

        $nameMatch = @(
            $items | Where-Object {
                [string]$_.namespaceShowName -ceq [string]$NAMESPACE
            }
        )

        if ($nameMatch.Count -gt 0) {
            Write-Err ("Configured NAMESPACE is a display name, not the namespace ID: " + $NAMESPACE)
            foreach ($item in $nameMatch) {
                $actualId = [string]$item.namespace
                if ([string]::IsNullOrWhiteSpace($actualId)) {
                    $actualId = "<public / empty>"
                }
                Write-Err ("Actual namespace ID: " + $actualId)
            }
            return $false
        }

        Write-Err ("Namespace ID does not exist: " + $NAMESPACE)
        Write-Info "Available namespaces:"

        foreach ($item in $items) {
            $id = [string]$item.namespace
            if ([string]::IsNullOrWhiteSpace($id)) {
                $id = "<public / empty>"
            }

            Write-Host ("  ID={0}  Name={1}" -f $id, [string]$item.namespaceShowName)
        }

        return $false
    }
    catch {
        $err = Get-HttpError $_
        Write-Warn ("Unable to validate namespace automatically. HTTP=" + $err.StatusCode)
        Write-Warn ("Detail: " + $err.Detail)
        Write-Warn "Import will continue, but make sure NAMESPACE is the namespace ID."
        return $true
    }
}

function Login-Nacos {
    param([string]$ApiMode)

    if (-not $ENABLE_AUTH) {
        Write-Warn "Authentication is disabled in this script."
        return $null
    }

    if ($ApiMode -eq "3") {
        $loginUrl = Get-NacosUrl "/v3/auth/user/login"
    }
    else {
        $loginUrl = Get-NacosUrl "/v1/auth/login"
    }

    Write-Info ("Logging in: " + $loginUrl)

    $body = @{
        username = $NACOS_USERNAME
        password = $NACOS_PASSWORD
    }

    try {
        $response = Invoke-RestMethod `
            -Uri $loginUrl `
            -Method Post `
            -Body $body `
            -ContentType "application/x-www-form-urlencoded" `
            -TimeoutSec $REQUEST_TIMEOUT_SECONDS `
            -ErrorAction Stop

        $token = [string]$response.accessToken

        if ([string]::IsNullOrWhiteSpace($token)) {
            throw "Login succeeded but accessToken is empty."
        }

        Write-Ok "Nacos login succeeded."
        return $token
    }
    catch {
        $err = Get-HttpError $_
        Write-Err ("Nacos login failed. HTTP=" + $err.StatusCode)
        Write-Err ("Detail: " + $err.Detail)
        throw
    }
}

function Publish-NacosConfig {
    param(
        [string]$ApiMode,
        [AllowNull()][string]$Token,
        [string]$DataId,
        [string]$Type,
        [string]$Content
    )

    if ($ApiMode -eq "3") {
        $url = Get-NacosUrl "/v3/admin/cs/config"

        $body = @{
            namespaceId = $NAMESPACE
            groupName = $GROUP
            dataId = $DataId
            type = $Type
            content = $Content
        }

        $headers = @{}

        if ($ENABLE_AUTH -and -not [string]::IsNullOrWhiteSpace($Token)) {
            $headers["Authorization"] = ("Bearer " + $Token)
        }

        $response = Invoke-RestMethod `
            -Uri $url `
            -Method Post `
            -Headers $headers `
            -Body $body `
            -ContentType "application/x-www-form-urlencoded" `
            -TimeoutSec $REQUEST_TIMEOUT_SECONDS `
            -ErrorAction Stop

        if (($response.code -ne 0) -or ([string]$response.data -ne "True")) {
            throw ("Nacos 3.x publish failed. code={0}, message={1}, data={2}" -f `
                $response.code, $response.message, $response.data)
        }

        return
    }

    $url = Get-NacosUrl "/v1/cs/configs"

    $body = @{
        dataId = $DataId
        group = $GROUP
        tenant = (Get-LegacyTenant)
        type = $Type
        content = $Content
    }

    if ($ENABLE_AUTH -and -not [string]::IsNullOrWhiteSpace($Token)) {
        $body["accessToken"] = $Token
    }

    $response = Invoke-RestMethod `
        -Uri $url `
        -Method Post `
        -Body $body `
        -ContentType "application/x-www-form-urlencoded" `
        -TimeoutSec $REQUEST_TIMEOUT_SECONDS `
        -ErrorAction Stop

    if ([string]$response -ne "True") {
        throw ("Nacos 1.x/2.x publish failed. Response: " + [string]$response)
    }
}

function Get-NacosConfig {
    param(
        [string]$ApiMode,
        [AllowNull()][string]$Token,
        [string]$DataId
    )

    $encodedDataId = Encode-UrlValue $DataId

    if ($ApiMode -eq "3") {
        $encodedGroup = Encode-UrlValue $GROUP
        $encodedNamespace = Encode-UrlValue $NAMESPACE

        $baseUrl = Get-NacosUrl "/v3/admin/cs/config"
        $url = "{0}?dataId={1}&groupName={2}&namespaceId={3}" -f `
            $baseUrl, $encodedDataId, $encodedGroup, $encodedNamespace

        $headers = @{}

        if ($ENABLE_AUTH -and -not [string]::IsNullOrWhiteSpace($Token)) {
            $headers["Authorization"] = ("Bearer " + $Token)
        }

        $response = Invoke-RestMethod `
            -Uri $url `
            -Method Get `
            -Headers $headers `
            -TimeoutSec $REQUEST_TIMEOUT_SECONDS `
            -ErrorAction Stop

        if ($response.code -ne 0) {
            throw ("Nacos 3.x read failed. code={0}, message={1}" -f `
                $response.code, $response.message)
        }

        return [string]$response.data.content
    }

    $encodedGroup = Encode-UrlValue $GROUP
    $encodedTenant = Encode-UrlValue (Get-LegacyTenant)

    $baseUrl = Get-NacosUrl "/v1/cs/configs"
    $url = "{0}?dataId={1}&group={2}&tenant={3}" -f `
        $baseUrl, $encodedDataId, $encodedGroup, $encodedTenant

    if ($ENABLE_AUTH -and -not [string]::IsNullOrWhiteSpace($Token)) {
        $encodedToken = Encode-UrlValue $Token
        $url = "{0}&accessToken={1}" -f $url, $encodedToken
    }

    $response = Invoke-WebRequest `
        -Uri $url `
        -Method Get `
        -UseBasicParsing `
        -TimeoutSec $REQUEST_TIMEOUT_SECONDS `
        -ErrorAction Stop

    return [string]$response.Content
}

function Normalize-Content {
    param([string]$Content)

    if ($null -eq $Content) {
        return ""
    }

    return (($Content -replace "`r`n", "`n") -replace "`r", "`n").TrimEnd("`n")
}

function Get-ImportValues {
    $valuesPath = Resolve-ScriptPath $IMPORT_VALUES_FILE

    if (-not (Test-Path -LiteralPath $valuesPath -PathType Leaf)) {
        throw ("Private import values file does not exist: " + $valuesPath)
    }

    try {
        $json = Get-Content -LiteralPath $valuesPath -Raw -Encoding UTF8 -ErrorAction Stop
        $config = ConvertFrom-Json -InputObject $json -ErrorAction Stop
    }
    catch {
        throw ("Unable to read private import values file: " + $_.Exception.Message)
    }

    $values = @{}

    foreach ($property in $config.PSObject.Properties) {
        if ([string]::IsNullOrWhiteSpace([string]$property.Value)) {
            throw ("Private import value cannot be blank: " + $property.Name)
        }

        $values[$property.Name] = [string]$property.Value
    }

    Write-Info ("Private import values file: " + $valuesPath)
    return $values
}

function Replace-ImportValues {
    param(
        [string]$Content,
        [string]$FileName,
        [hashtable]$ImportValues
    )

    $replacedContent = $Content

    # YAML template values use quoted placeholders. Remove the template quotes
    # together with the placeholder so the imported YAML contains raw values.
    if ($FileName -notmatch "\.json$") {
        $replacedContent = [regex]::Replace(
            $replacedContent,
            '"@([A-Z][A-Z0-9_]*)@"',
            {
                param($match)
                $name = $match.Groups[1].Value

                if (-not $ImportValues.ContainsKey($name)) {
                    throw ("No import value is configured for placeholder: @" + $name + "@")
                }

                return [string]$ImportValues[$name]
            }
        )
    }

    $replacedContent = [regex]::Replace(
        $replacedContent,
        "@[A-Z][A-Z0-9_]*@",
        {
            param($match)
            $name = $match.Value.Trim("@")

            if (-not $ImportValues.ContainsKey($name)) {
                throw ("No import value is configured for placeholder: " + $match.Value)
            }

            return [string]$ImportValues[$name]
        }
    )

    if ($FileName -ieq "peach-datasource.yml") {
        $replacedContent = [regex]::Replace(
            $replacedContent,
            "(?m)^(\s*username:\s*).*?$",
            {
                param($match)
                return ($match.Groups[1].Value + [string]$ImportValues["MYSQL_USERNAME"])
            }
        )
    }

    if ($replacedContent -match "@[A-Z][A-Z0-9_]*@") {
        throw ("Unresolved import placeholder remains in: " + $FileName)
    }

    return $replacedContent
}

function Main {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host " Nacos Configuration Import" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan

    $configPath = Resolve-ScriptPath $CONFIG_DIR

    Write-Info ("Script directory : " + $BASE_DIR)
    Write-Info ("Config directory : " + $configPath)
    Write-Info ("Nacos server     : " + $NACOS_SERVER)
    Write-Info ("Namespace ID     : " + $NAMESPACE)
    Write-Info ("Group            : " + $GROUP)

    if (-not (Test-Path -LiteralPath $configPath -PathType Container)) {
        Write-Err ("Config directory does not exist: " + $configPath)
        return 1
    }

    $files = @(
        Get-ChildItem -LiteralPath $configPath -File -ErrorAction Stop |
        Where-Object {
            $ext = $_.Extension.ToLowerInvariant()
            ($ext -eq ".yml") -or ($ext -eq ".yaml") -or ($ext -eq ".json")
        } |
        Sort-Object Name
    )

    if ($files.Count -eq 0) {
        Write-Err "No .yml, .yaml or .json files found."
        return 2
    }

    Write-Ok ("Found {0} configuration file(s)." -f $files.Count)

    $duplicates = @(
        $files |
        Group-Object Name |
        Where-Object { $_.Count -gt 1 }
    )

    if ($duplicates.Count -gt 0) {
        Write-Err "Duplicate DataId detected. Import stopped."
        foreach ($item in $duplicates) {
            Write-Err ("Duplicate: " + $item.Name)
        }
        return 3
    }

    Write-Info "Loading private import values file."
    $importValues = Get-ImportValues

    $apiMode = Get-NacosApiMode
    Write-Info ("API mode: Nacos " + $apiMode + ".x")

    $token = Login-Nacos $apiMode

    $namespaceOk = Test-NacosNamespace `
        -ApiMode $apiMode `
        -Token $token

    if (-not $namespaceOk) {
        Write-Err "Import stopped because the configured namespace ID is invalid."
        return 4
    }

    $successCount = 0
    $failCount = 0
    $skipCount = 0
    $verifyFailCount = 0

    foreach ($file in $files) {
        Write-Host ""
        Write-Info ("Importing: " + $file.Name)

        if ($file.Length -eq 0) {
            Write-Warn ("Skipped empty file: " + $file.Name)
            $skipCount++
            continue
        }

        switch ($file.Extension.ToLowerInvariant()) {
            ".yml"  { $type = "yaml" }
            ".yaml" { $type = "yaml" }
            ".json" { $type = "json" }
            default {
                $skipCount++
                continue
            }
        }

        try {
            $content = Get-Content `
                -LiteralPath $file.FullName `
                -Raw `
                -Encoding UTF8 `
                -ErrorAction Stop

            $content = Replace-ImportValues `
                -Content $content `
                -FileName $file.Name `
                -ImportValues $importValues

            if ([string]::IsNullOrWhiteSpace($content)) {
                Write-Warn ("Skipped blank file: " + $file.Name)
                $skipCount++
                continue
            }

            if ($VALIDATE_JSON -and ($type -eq "json")) {
                try {
                    $null = ConvertFrom-Json -InputObject $content -ErrorAction Stop
                }
                catch {
                    throw ("Invalid JSON: " + $_.Exception.Message)
                }
            }

            Publish-NacosConfig `
                -ApiMode $apiMode `
                -Token $token `
                -DataId $file.Name `
                -Type $type `
                -Content $content

            Write-Ok ("Published successfully: " + $file.Name)
            $successCount++

            if ($VERIFY_AFTER_IMPORT) {
                $verified = $false
                $lastVerifyError = $null

                for ($attempt = 1; $attempt -le $VERIFY_RETRY_COUNT; $attempt++) {
                    try {
                        $remoteContent = Get-NacosConfig `
                            -ApiMode $apiMode `
                            -Token $token `
                            -DataId $file.Name

                        $localNormalized = Normalize-Content $content
                        $remoteNormalized = Normalize-Content $remoteContent

                        if ($localNormalized -ceq $remoteNormalized) {
                            $verified = $true
                            break
                        }

                        $lastVerifyError = "Remote content is not yet equal to local content."
                    }
                    catch {
                        $verifyErr = Get-HttpError $_
                        $lastVerifyError = $verifyErr.Detail
                    }

                    if ($attempt -lt $VERIFY_RETRY_COUNT) {
                        Start-Sleep -Milliseconds $VERIFY_RETRY_INTERVAL_MS
                    }
                }

                if ($verified) {
                    Write-Ok ("Verified successfully: " + $file.Name)
                }
                else {
                    Write-Warn ("Published, but verification did not succeed: " + $file.Name)
                    Write-Warn ("Verification detail: " + $lastVerifyError)
                    Write-Warn ("You can check this DataId in the Nacos console.")
                    $verifyFailCount++
                }
            }
        }
        catch {
            $err = Get-HttpError $_
            Write-Err ("Import failed: " + $file.Name)

            if ($null -ne $err.StatusCode) {
                Write-Err ("HTTP status: " + $err.StatusCode)
            }

            Write-Err ("Detail: " + $err.Detail)
            $failCount++
        }
    }

    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host " Import Summary" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ("Total   : " + $files.Count)
    Write-Host ("Published: " + $successCount) -ForegroundColor Green
    Write-Host ("Failed   : " + $failCount) -ForegroundColor Red
    Write-Host ("Skipped  : " + $skipCount) -ForegroundColor Yellow

    if ($VERIFY_AFTER_IMPORT) {
        Write-Host ("Verify warnings: " + $verifyFailCount) -ForegroundColor Yellow
    }

    Write-Host "============================================" -ForegroundColor Cyan

    if ($failCount -gt 0) {
        return 10
    }

    if ($verifyFailCount -gt 0) {
        return 11
    }

    return 0
}

$exitCode = 99

try {
    $exitCode = Main
}
catch {
    Write-Host ""
    Write-Err "Unhandled error."
    Write-Err $_.Exception.Message

    if ($null -ne $_.InvocationInfo) {
        Write-Err $_.InvocationInfo.PositionMessage
    }

    $exitCode = 99
}
finally {
    Write-Host ""
    Write-Host ("Script finished. Exit code: " + $exitCode) -ForegroundColor Cyan

    if ($PAUSE_ON_EXIT) {
        Write-Host ""
        [void](Read-Host "Press Enter to exit")
    }
}

exit $exitCode
