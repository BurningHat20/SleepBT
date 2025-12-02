# Create directories
$dirs = @(
    "app\src\main\res\drawable",
    "app\src\main\res\mipmap-anydpi-v26"
)

foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force
    }
}

# Create ic_launcher_background.xml
$bg = @"
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#2196F3"
        android:pathData="M0,0h108v108h-108z" />
</vector>
"@
Set-Content -Path "app\src\main\res\drawable\ic_launcher_background.xml" -Value $bg

# Create ic_launcher_foreground.xml
$fg = @"
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.5"
        android:scaleY="0.5"
        android:translateX="27"
        android:translateY="27">
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M54,24C38.536,24 26,36.536 26,52s12.536,28 28,28 28,-12.536 28,-28S69.464,24 54,24zM54,74c-12.15,0 -22,-9.85 -22,-22s9.85,-22 22,-22 22,9.85 22,22 -9.85,22 -22,22z" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M48,42h4v18h-4z" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M54,42h4v18h-4z" />
    </group>
</vector>
"@
Set-Content -Path "app\src\main\res\drawable\ic_launcher_foreground.xml" -Value $fg

# Create ic_launcher.xml
$launcher = @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"@
Set-Content -Path "app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml" -Value $launcher

# Create ic_launcher_round.xml
Set-Content -Path "app\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml" -Value $launcher

Write-Host "✅ All icon resources created successfully!" -ForegroundColor Green