package com.eimemes.chat;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

public class AppDialog {

    // ── Clear chats confirm dialog ────────────────────────────────
    // Matches web: title + desc + divider + Cancel | Delete all
    public static void showConfirm(Context ctx,
                                   String title,
                                   String desc,
                                   String confirmText,
                                   boolean destructive,
                                   Runnable onConfirm) {

        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Dim background like web's rgba(0,0,0,0.5)
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.dimAmount = 0.5f;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Width = match parent with margins (same as web's min(320px, 100vw-48px))
        dialog.getWindow().setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );

        TextView tvTitle   = dialog.findViewById(R.id.dialogTitle);
        TextView tvDesc    = dialog.findViewById(R.id.dialogDesc);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnConfirm= dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText(title);
        tvDesc.setText(desc);
        btnConfirm.setText(confirmText);
        if (!destructive) btnConfirm.setTextColor(0xFF5e9cff); // accent for non-destructive

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> { dialog.dismiss(); onConfirm.run(); });

        dialog.show();

        // Scale-in animation matching web's transform: scale(0.93) → scale(1)
        // Must run AFTER show() so the window is attached
        View card = dialog.findViewById(R.id.dialogTitle).getRootView();
        card.setScaleX(0.93f);
        card.setScaleY(0.93f);
        card.setAlpha(0f);
        card.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(200)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();
    }

    // ── Sign out dialog ───────────────────────────────────────────
    // Matches web: icon + title + desc + red Sign out btn + Cancel btn
    public static void showSignOut(Context ctx, Runnable onConfirm) {

        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_signout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.dimAmount = 0.5f;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );

        View btnConfirm = dialog.findViewById(R.id.btnSignOutConfirm);
        View btnCancel  = dialog.findViewById(R.id.btnSignOutCancel);

        btnCancel.setOnClickListener(v  -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> { dialog.dismiss(); onConfirm.run(); });

        dialog.show();

        // modalPop animation: scale(0.92) translateY(8px) → scale(1) translateY(0)
        // Must run AFTER show() so the window is attached
        View card = dialog.getWindow().getDecorView();
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        card.setTranslationY(24f);
        card.setAlpha(0f);
        card.animate()
            .scaleX(1f).scaleY(1f)
            .translationY(0f).alpha(1f)
            .setDuration(220)
            .setInterpolator(new OvershootInterpolator(1.4f))
            .start();
    }
}
