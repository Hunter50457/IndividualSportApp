package com.example.individualsportapp;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Locale;

import android.app.Activity;

public class MainActivity extends Activity {
    private static final String PREFS = "sport_prefs";
    private static final int GREEN = Color.rgb(0, 200, 83);
    private static final int DARK = Color.rgb(16, 24, 32);
    private static final int CARD = Color.rgb(30, 42, 51);

    private SharedPreferences prefs;
    private TextView greetingText;
    private TextView progressText;
    private TextView timerText;
    private ProgressBar progressBar;
    private LinearLayout planListContainer;
    private int dailyGoal;
    private int completedMinutes;
    private CountDownTimer timer;
    private boolean timerRunning = false;
    private long timerMillisLeft = 20 * 60 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        dailyGoal = prefs.getInt("daily_goal", 45);
        completedMinutes = prefs.getInt("completed_minutes", 0);
        buildUi();
        updateProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(DARK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(28), dp(18), dp(28));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🏋️ Мой Спорт");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        greetingText = new TextView(this);
        String name = prefs.getString("name", "спортсмен");
        greetingText.setText("Привет, " + name + "! Сегодня отличный день для тренировки.");
        greetingText.setTextColor(Color.LTGRAY);
        greetingText.setTextSize(16);
        greetingText.setPadding(0, dp(8), 0, dp(16));
        root.addView(greetingText);

        addProfileCard(root);
        addProgressCard(root);
        addTimerCard(root);
        addBmiCard(root);
        addPlanCard(root);

        setContentView(scrollView);
    }

    private void addProfileCard(LinearLayout root) {
        LinearLayout card = card(root, "👤 Индивидуальная настройка");
        Button setup = button("Настроить имя и цель");
        setup.setOnClickListener(v -> showSettingsDialog());
        card.addView(setup);
    }

    private void addProgressCard(LinearLayout root) {
        LinearLayout card = card(root, "🎯 Цель на сегодня");
        progressText = text("", 16, Color.WHITE, false);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        card.addView(progressText);
        card.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(12)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(12), 0, 0);

        Button plus10 = button("+10 мин");
        plus10.setOnClickListener(v -> addMinutes(10));
        Button plus20 = button("+20 мин");
        plus20.setOnClickListener(v -> addMinutes(20));
        Button reset = button("Сброс");
        reset.setOnClickListener(v -> { completedMinutes = 0; saveProgress(); updateProgress(); });

        buttons.addView(plus10, weightParams());
        buttons.addView(plus20, weightParams());
        buttons.addView(reset, weightParams());
        card.addView(buttons);
    }

    private void addTimerCard(LinearLayout root) {
        LinearLayout card = card(root, "⏱ Таймер тренировки");
        timerText = text(formatTime(timerMillisLeft), 34, GREEN, true);
        timerText.setGravity(Gravity.CENTER);
        card.addView(timerText);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button start = button("Старт/Пауза");
        Button reset = button("20:00");
        start.setOnClickListener(v -> toggleTimer());
        reset.setOnClickListener(v -> resetTimer());
        buttons.addView(start, weightParams());
        buttons.addView(reset, weightParams());
        card.addView(buttons);
    }

    private void addBmiCard(LinearLayout root) {
        LinearLayout card = card(root, "⚖️ Калькулятор ИМТ");
        EditText height = input("Рост, см");
        EditText weight = input("Вес, кг");
        TextView result = text("Введите рост и вес", 16, Color.LTGRAY, false);
        Button calc = button("Рассчитать ИМТ");
        calc.setOnClickListener(v -> {
            try {
                double h = Double.parseDouble(height.getText().toString()) / 100.0;
                double w = Double.parseDouble(weight.getText().toString());
                if (h <= 0 || w <= 0) throw new Exception();
                double bmi = w / (h * h);
                String category = bmi < 18.5 ? "ниже нормы" : bmi < 25 ? "норма" : bmi < 30 ? "выше нормы" : "ожирение";
                result.setText("ИМТ: " + new DecimalFormat("0.0").format(bmi) + " — " + category);
            } catch (Exception e) {
                Toast.makeText(this, "Проверьте данные", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(height);
        card.addView(weight);
        card.addView(calc);
        card.addView(result);
    }

    private void addPlanCard(LinearLayout root) {
        LinearLayout card = card(root, "📋 План тренировки");

        planListContainer = new LinearLayout(this);
        planListContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(planListContainer);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(12), 0, 0);

        Button editPlan = button("Изменить план");
        editPlan.setOnClickListener(v -> showPlanDialog());

        Button resetPlan = button("Стандартный");
        resetPlan.setOnClickListener(v -> {
            prefs.edit().putString("training_plan", getDefaultPlan()).apply();
            renderTrainingPlan();
            Toast.makeText(this, "План сброшен", Toast.LENGTH_SHORT).show();
        });

        buttons.addView(editPlan, weightParams());
        buttons.addView(resetPlan, weightParams());
        card.addView(buttons);

        renderTrainingPlan();
    }

    private String getDefaultPlan() {
        return "Разминка — 5 минут\n" +
                "Приседания — 3 × 15\n" +
                "Отжимания — 3 × 10\n" +
                "Планка — 3 × 40 секунд\n" +
                "Заминка и растяжка — 5 минут";
    }

    private String getTrainingPlan() {
        return prefs.getString("training_plan", getDefaultPlan());
    }

    private void renderTrainingPlan() {
        if (planListContainer == null) return;
        planListContainer.removeAllViews();

        String plan = getTrainingPlan();
        String[] items = plan.split("\\n");

        boolean hasItems = false;
        for (String item : items) {
            String cleanItem = item.trim();
            if (cleanItem.isEmpty()) continue;
            hasItems = true;
            TextView row = text("• " + cleanItem, 16, Color.WHITE, false);
            row.setPadding(0, dp(4), 0, dp(4));
            planListContainer.addView(row);
        }

        if (!hasItems) {
            TextView empty = text("План пуст. Нажмите «Изменить план», чтобы добавить упражнения.", 16, Color.LTGRAY, false);
            empty.setPadding(0, dp(4), 0, dp(4));
            planListContainer.addView(empty);
        }
    }

    private void showPlanDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), 0, dp(10), 0);

        TextView hint = text("Каждое упражнение пишите с новой строки:", 14, Color.DKGRAY, false);
        EditText planInput = new EditText(this);
        planInput.setText(getTrainingPlan());
        planInput.setMinLines(6);
        planInput.setGravity(Gravity.TOP);
        planInput.setSingleLine(false);
        planInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        box.addView(hint);
        box.addView(planInput);

        new AlertDialog.Builder(this)
                .setTitle("Изменить план")
                .setView(box)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newPlan = planInput.getText().toString().trim();
                    prefs.edit().putString("training_plan", newPlan).apply();
                    renderTrainingPlan();
                    Toast.makeText(this, "План сохранён", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private LinearLayout card(LinearLayout root, String header) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(CARD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(14));
        root.addView(card, params);

        TextView h = text(header, 20, Color.WHITE, true);
        h.setPadding(0, 0, 0, dp(10));
        card.addView(h);
        return card;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setHintTextColor(Color.GRAY);
        editText.setTextColor(Color.WHITE);
        editText.setSingleLine(true);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return editText;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private void showSettingsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), 0, dp(10), 0);
        EditText name = new EditText(this);
        name.setHint("Ваше имя");
        name.setText(prefs.getString("name", "спортсмен"));
        EditText goal = new EditText(this);
        goal.setHint("Цель в минутах в день");
        goal.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        goal.setText(String.valueOf(dailyGoal));
        box.addView(name);
        box.addView(goal);

        new AlertDialog.Builder(this)
                .setTitle("Профиль")
                .setView(box)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newName = name.getText().toString().trim();
                    if (newName.isEmpty()) newName = "спортсмен";
                    int newGoal = dailyGoal;
                    try { newGoal = Math.max(5, Integer.parseInt(goal.getText().toString())); } catch (Exception ignored) {}
                    dailyGoal = newGoal;
                    prefs.edit().putString("name", newName).putInt("daily_goal", dailyGoal).apply();
                    greetingText.setText("Привет, " + newName + "! Сегодня отличный день для тренировки.");
                    updateProgress();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addMinutes(int minutes) {
        completedMinutes += minutes;
        saveProgress();
        updateProgress();
    }

    private void saveProgress() {
        prefs.edit().putInt("completed_minutes", completedMinutes).apply();
    }

    private void updateProgress() {
        int percent = Math.min(100, Math.round(completedMinutes * 100f / dailyGoal));
        progressBar.setProgress(percent);
        progressText.setText(String.format(Locale.getDefault(), "%d из %d минут — %d%%", completedMinutes, dailyGoal, percent));
    }

    private void toggleTimer() {
        if (timerRunning) {
            if (timer != null) timer.cancel();
            timerRunning = false;
            return;
        }
        timerRunning = true;
        timer = new CountDownTimer(timerMillisLeft, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                timerMillisLeft = millisUntilFinished;
                timerText.setText(formatTime(timerMillisLeft));
            }
            @Override public void onFinish() {
                timerRunning = false;
                timerMillisLeft = 0;
                timerText.setText("00:00");
                addMinutes(20);
                Toast.makeText(MainActivity.this, "Тренировка завершена! +20 минут", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void resetTimer() {
        if (timer != null) timer.cancel();
        timerRunning = false;
        timerMillisLeft = 20 * 60 * 1000L;
        timerText.setText(formatTime(timerMillisLeft));
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
