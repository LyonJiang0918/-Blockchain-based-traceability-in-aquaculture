# Download FISCO BCOS binary
$ProgressPreference = 'SilentlyContinue'
$url = "https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/v3.11.0/FISCO-BCOS-3.11.0-linux-x86_64.tar.gz"
$output = "$env:TEMP\FISCO-BCOS.tar.gz"

Write-Host "Downloading FISCO BCOS from $url..."
try {
    Invoke-WebRequest -Uri $url -OutFile $output -UserAgent "Mozilla/5.0"
    Write-Host "Downloaded to $output"
    $size = (Get-Item $output).Length
    Write-Host "File size: $size bytes"
} catch {
    Write-Host "Download failed: $_"
}
