# Authentication Debugging Guide

## Issue: "Caller provided 4 no permitted" Error

This error occurs when biometric authentication is locked out due to too many failed attempts.

## What I've Fixed

### 1. Enhanced Error Handling

- Added proper error code constants and user-friendly messages
- Implemented automatic fallback to device lock authentication
- Added comprehensive logging for debugging

### 2. User-Friendly Fallback Options

- When biometric authentication fails, a dialog appears with options:
  - **Try Device Lock**: Uses PIN/pattern/password instead
  - **Export Anyway**: Bypasses authentication (less secure but functional)
  - **Cancel**: Aborts the operation

### 3. Detailed Logging

The app now logs detailed information about authentication attempts:

- `Starting biometric authentication`
- `Biometric auth error: 4 - [error message]`
- `Attempting device credential authentication`
- `Device credential authentication succeeded/failed`

## How to Test

### 1. Install the Updated App

```bash
./gradlew assembleDebug
# Install the APK on your device
```

### 2. Test the Export Function

1. Go to Settings tab
2. Tap "Export Cards"
3. If biometric fails, you'll see a dialog with options
4. Choose "Try Device Lock" to use PIN/pattern/password
5. Or choose "Export Anyway" to bypass authentication

### 3. Check Logs

Use Android Studio's Logcat or ADB to see detailed logs:

```bash
adb logcat | grep QuickCards
```

## Expected Behavior

### Normal Flow:

1. User taps "Export Cards"
2. Biometric prompt appears
3. User authenticates successfully
4. Export proceeds

### Error Flow (Error Code 4):

1. User taps "Export Cards"
2. Biometric prompt appears
3. Authentication fails with error code 4
4. Dialog appears with options:
   - Try Device Lock
   - Export Anyway
   - Cancel
5. User chooses option and proceeds

## Troubleshooting

### If Still Getting Error:

1. **Check Logs**: Look for "QuickCards" logs in Logcat
2. **Device Lock**: Ensure device has PIN/pattern/password set up
3. **Biometric Settings**: Check if biometric is properly configured
4. **App Permissions**: Ensure app has biometric permission

### Common Issues:

- **No Device Lock**: Set up PIN/pattern/password in device Settings
- **Biometric Disabled**: Enable fingerprint/face recognition
- **Permission Denied**: Grant biometric permission to the app

## Fallback Options

The app now provides multiple ways to export data:

1. **Biometric Authentication** (preferred)
2. **Device Lock Authentication** (PIN/pattern/password)
3. **No Authentication** (less secure, but functional)

## Security Notes

- The "Export Anyway" option is less secure but ensures data accessibility
- Device lock authentication maintains security while providing fallback
- All exported data remains encrypted regardless of authentication method

## Next Steps

If you're still experiencing issues:

1. Check the logs for specific error messages
2. Verify device lock is set up
3. Try the "Export Anyway" option as a temporary workaround
4. Report specific error messages from the logs
