package com.x1unix.avi.updateManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.rollbar.android.Rollbar;
import com.x1unix.avi.R;
import com.x1unix.avi.UpdateDownloaderActivity;
import com.x1unix.avi.model.AviSemVersion;

public class OTAUpdateChecker {
    public static void checkForUpdates(final OTAStateListener otaEventListener, final boolean allowNightlies) {
        OTARepoClientInterface repoClient = OTARestClient.getClient().create(OTARepoClientInterface.class);
        Call<AviSemVersion> call = repoClient.getLatestRelease();
        call.enqueue(new Callback<AviSemVersion>() {
            @Override
            public void onResponse(Call<AviSemVersion>call, Response<AviSemVersion> response) {
                int statusCode = response.code();
                try {
                    AviSemVersion receivedVersion = response.body();
                    AviSemVersion current = AviSemVersion.getApplicationVersion();

                    boolean isNew = false;

                    try {
                        isNew = BuildParser.compareBuild(receivedVersion.getReleaseDate());
                    } catch(Exception ex) {
                        isNew = true;
                    }

                    boolean isStable = receivedVersion.isStable();
                    boolean isSuitable = (isStable || allowNightlies);

                    if (isNew && isSuitable) {
                        otaEventListener.onUpdateAvailable(receivedVersion, current);
                    } else {
                        otaEventListener.onUpdateMissing(receivedVersion, current);
                    }
                } catch (Exception ex) {
                    otaEventListener.onError(ex);
                }
            }

            @Override
            public void onFailure(Call<AviSemVersion>call, Throwable t) {
                // Log error here since request failed
                otaEventListener.onError(t);
            }
        });
    }

    public static AlertDialog.Builder makeDialog(final Context owner, final AviSemVersion newVer) {
        Resources res = owner.getResources();
        AlertDialog.Builder dialInstallUpdate = new AlertDialog.Builder(owner);
        String modConfimText = res.getString(R.string.upd_confirm);
        modConfimText = modConfimText.replace("@version", newVer.toString());

        dialInstallUpdate.setMessage(modConfimText);
        dialInstallUpdate.setTitle(res.getString(R.string.upd_new_available))
                .setCancelable(false)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        return dialInstallUpdate;
    }
}
