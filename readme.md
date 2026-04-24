# DC SSL Unpinned APK

Contact: https://t.me/yaziraof

This repository contains the patched APK built from:

- Package: `com.discore` (modified)
- Version: `306.13 - Stable`
- Version code: `306013`
- Final APK: `discord-patched.apk`
- Final APK SHA-256: `0E587B3BBCCA42EDA2EF9972C70160CB43F2CF58A131C13CC25E28D63B6029A3`

## Summary

The SSL unpin was done by patching the decompiled APK in the `work` folder, rebuilding the changed dex/resource files, repacking the APK, zipaligning it, and signing it again.

The SSL-related changes are:

1. Android network security config was changed to trust user-installed CAs at runtime.
2. OkHttp certificate pin checks were bypassed by returning before the pin verification logic executes.
3. React Native WebView SSL errors were changed to proceed instead of cancel.

## Network Security Config

Original `res/xml/network_security_config.xml` only trusted user certificates inside `debug-overrides`, so user CAs were not trusted for the release app path.

The patched config in `work/decoded/res/xml/network_security_config.xml` uses a base config:

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

This makes Android's platform trust manager accept both system CAs and user-installed CAs, which is required for proxy certificates such as Burp, mitmproxy, or Charles.

## OkHttp Certificate Pinning

The OkHttp certificate pinner class was found in:

```text
work/smali5/rv/c.smali
```

Two pin-check methods were patched with an early `return-void`:

```smali
.method public final a(Ljava/lang/String;Ljava/util/List;)V
    .registers 4

    return-void
```

```smali
.method public final b(Ljava/lang/String;Lkotlin/jvm/functions/Function0;)V
    .registers 8

    return-void
```

Before the patch, these methods reached the certificate pin comparison path and could throw `javax.net.ssl.SSLPeerUnverifiedException` with a certificate pinning failure. Returning immediately prevents OkHttp from enforcing the pinned certificate set after the TLS chain has already been accepted by the trust manager.

## WebView SSL Handling

The React Native WebView client SSL callback was patched in:

```text
work/smali_classes4/com/reactnativecommunity/webview/RNCWebViewManager$h.smali
```

The public `onReceivedSslError(...)` method now proceeds immediately:

```smali
.method public onReceivedSslError(Landroid/webkit/WebView;Landroid/webkit/SslErrorHandler;Landroid/net/http/SslError;)V
    .registers 7

    invoke-virtual {p2}, Landroid/webkit/SslErrorHandler;->proceed()V

    return-void
.end method
```

The original handler body was kept under `onReceivedSslError_orig(...)` in the working tree. The active callback no longer cancels loads on WebView SSL errors.

## Repack Flow

The working folder shows the final APK was produced by:

1. Decoding the original APK with apktool.
2. Editing the smali/resources listed above.
3. Rebuilding the changed dex files.
4. Replacing the changed APK entries with `work/PatchApk.java`.
5. Removing stale original signature metadata during repack.
6. Running zipalign/signing to produce `discord-patched.apk`.

Verification with `apksigner`:

```text
Verifies
Verified using v2 scheme: true
Verified using v3 scheme: true
Number of signers: 1
```

Only the SSL unpinning behavior is documented here.
