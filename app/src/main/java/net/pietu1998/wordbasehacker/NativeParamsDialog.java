package net.pietu1998.wordbasehacker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import net.pietu1998.wordbasehacker.solver.NativeSolver;

public class NativeParamsDialog {

    private NativeSolver.Params params;

    private AlertDialog dialog;
    private final SeekBar maxDepth;
    private final SeekBar maxBreadth;
    private final SeekBar maxSteps;
    private final SeekBar speedupFactor;
    private final TextView maxDepthView;
    private final TextView maxBreadthView;
    private final TextView maxStepsView;
    private final TextView speedupFactorView;

    private NewParamsListener newParamsListener = null;

    private boolean updatingFields = false;

    public NativeParamsDialog(Context context) {
        params = NativeSolver.Params.fromPreferences(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View layout = View.inflate(context, R.layout.native_param_dialog, null);
        builder.setView(layout);
        builder.setTitle(R.string.pref_native_params);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, (dialog1, which) -> {
            params.saveToPreferences(dialog.getContext());
            if (newParamsListener != null)
                newParamsListener.onNewParamsAccepted(params);
        });

        maxDepth = layout.findViewById(R.id.max_depth);
        maxBreadth = layout.findViewById(R.id.max_breadth);
        maxSteps = layout.findViewById(R.id.max_steps);
        speedupFactor = layout.findViewById(R.id.speedup_factor);

        maxDepthView = layout.findViewById(R.id.max_depth_show);
        maxBreadthView = layout.findViewById(R.id.max_breadth_show);
        maxStepsView = layout.findViewById(R.id.max_steps_show);
        speedupFactorView = layout.findViewById(R.id.speedup_factor_show);

        updateBars();
        updateParams();

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateParams();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        maxDepth.setOnSeekBarChangeListener(listener);
        maxBreadth.setOnSeekBarChangeListener(listener);
        maxSteps.setOnSeekBarChangeListener(listener);
        speedupFactor.setOnSeekBarChangeListener(listener);

        Button defaults = layout.findViewById(R.id.defaults);
        defaults.setOnClickListener(v -> {
            params = NativeSolver.Params.DEFAULT;
            updateBars();
            updateParams();
        });

        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }

    public void setNewParamsListener(NewParamsListener newParamsListener) {
        this.newParamsListener = newParamsListener;
    }

    @SuppressLint("SetTextI18n")
    private void updateBars() {
        updatingFields = true;
        maxDepth.setProgress(params.maxDepth);
        maxBreadth.setProgress(params.maxBreadth - 1);
        maxSteps.setProgress(params.maxSteps / 1000 - 1);
        speedupFactor.setProgress(params.speedupFactor - 128);
        updatingFields = false;
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateParams() {
        if (updatingFields)
            return;
        int maxDepthVal = maxDepth.getProgress();
        maxDepthView.setText(Integer.toString(maxDepthVal));
        int maxBreadthVal = maxBreadth.getProgress() + 1;
        maxBreadthView.setText(Integer.toString(maxBreadthVal));
        int maxStepsVal = maxSteps.getProgress() * 1000 + 1000;
        maxStepsView.setText(Integer.toString(maxStepsVal));
        int speedupFactorVal = speedupFactor.getProgress() + 128;
        speedupFactorView.setText(String.format("%.2f", speedupFactorVal / 256.0));
        params = new NativeSolver.Params(maxDepthVal, maxBreadthVal, maxStepsVal, speedupFactorVal);
    }

    public interface NewParamsListener {
        void onNewParamsAccepted(NativeSolver.Params params);
    }
}
