package net.pietu1998.wordbasehacker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.pietu1998.wordbasehacker.solver.Scoring;

public class ScoringDialog {

    private Scoring scoring;

    private final AlertDialog dialog;
    private final EditText lettersBox;
    private final EditText minesBox;
    private final EditText tilesPlrBox;
    private final EditText tilesOppBox;
    private final EditText progressPlrBox;
    private final EditText progressOppBox;
    private final EditText winBox;
    private final EditText loseBox;

    private NewScoringListener newScoringListener = null;

    private boolean updatingFields = false;

    public ScoringDialog(Context context, Scoring initial, boolean useNative) {
        scoring = initial;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View layout = View.inflate(context, R.layout.scoring_dialog, null);
        builder.setView(layout);
        builder.setTitle(R.string.scoring);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, (dialog1, which) -> {
            if (newScoringListener != null)
                newScoringListener.onNewScoringAccepted(scoring);
        });

        lettersBox = layout.findViewById(R.id.lettersBox);
        minesBox = layout.findViewById(R.id.minesBox);
        tilesPlrBox = layout.findViewById(R.id.tilesPlrBox);
        tilesOppBox = layout.findViewById(R.id.tilesOppBox);
        progressPlrBox = layout.findViewById(R.id.progressPlrBox);
        progressOppBox = layout.findViewById(R.id.progressOppBox);
        winBox = layout.findViewById(R.id.winBox);
        loseBox = layout.findViewById(R.id.loseBox);

        updateFields();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateScoring();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        lettersBox.addTextChangedListener(watcher);
        minesBox.addTextChangedListener(watcher);
        tilesPlrBox.addTextChangedListener(watcher);
        tilesOppBox.addTextChangedListener(watcher);
        progressPlrBox.addTextChangedListener(watcher);
        progressOppBox.addTextChangedListener(watcher);
        winBox.addTextChangedListener(watcher);
        loseBox.addTextChangedListener(watcher);


        layout.findViewById(R.id.minesRow).setVisibility(useNative ? View.GONE : View.VISIBLE);
        layout.findViewById(R.id.loseRow).setVisibility(useNative ? View.VISIBLE : View.GONE);

        Button defaults = layout.findViewById(R.id.defaults);
        defaults.setOnClickListener(v -> {
            scoring = Scoring.DEFAULT;
            updateFields();
        });

        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }

    public void setNewScoringListener(NewScoringListener newScoringListener) {
        this.newScoringListener = newScoringListener;
    }

    @SuppressLint("SetTextI18n")
    private void updateFields() {
        updatingFields = true;
        lettersBox.setText(Integer.toString(scoring.letter));
        minesBox.setText(Integer.toString(scoring.mine));
        tilesPlrBox.setText(Integer.toString(scoring.tileGain));
        tilesOppBox.setText(Integer.toString(scoring.tileKill));
        progressPlrBox.setText(Integer.toString(scoring.progressGain));
        progressOppBox.setText(Integer.toString(scoring.progressKill));
        winBox.setText(Integer.toString(scoring.winBonus));
        loseBox.setText(Integer.toString(scoring.loseMinus));
        updatingFields = false;
    }

    private void updateScoring() {
        if (updatingFields)
            return;
        try {
            int letter = Integer.parseInt(lettersBox.getText().toString());
            int mine = Integer.parseInt(minesBox.getText().toString());
            int tileGain = Integer.parseInt(tilesPlrBox.getText().toString());
            int tileKill = Integer.parseInt(tilesOppBox.getText().toString());
            int progressGain = Integer.parseInt(progressPlrBox.getText().toString());
            int progressKill = Integer.parseInt(progressOppBox.getText().toString());
            int winBonus = Integer.parseInt(winBox.getText().toString());
            int loseMinus = Integer.parseInt(loseBox.getText().toString());
            scoring = new Scoring(letter, mine, tileGain, tileKill, progressGain, progressKill, winBonus, loseMinus);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        } catch (NumberFormatException e) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    public interface NewScoringListener {
        void onNewScoringAccepted(Scoring scoring);
    }

}
