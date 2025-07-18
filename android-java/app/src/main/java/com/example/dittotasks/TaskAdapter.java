package com.example.dittotasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks = new ArrayList<>();
    private OnTaskToggleListener toggleListener;
    private OnTaskDeleteListener deleteListener;
    private OnTaskPressListener longPressListener;

    public interface OnTaskToggleListener {
        void onTaskToggle(Task task, boolean isChecked);
    }

    public interface OnTaskDeleteListener {
        void onTaskDelete(Task task);
    }

    public interface OnTaskPressListener {
        void onTaskPress(Task task);
    }

    public void setOnTaskToggleListener(OnTaskToggleListener listener) {
        this.toggleListener = listener;
    }

    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnTaskLongPressListener(OnTaskPressListener listener) {
        this.longPressListener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView textView;
        private ImageButton deleteButton;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.task_checkbox);
            textView = itemView.findViewById(R.id.task_text);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        void bind(final Task task) {
            textView.setText(task.getTitle());
            checkBox.setChecked(task.isDone());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (toggleListener != null) {
                    toggleListener.onTaskToggle(task, isChecked);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onTaskDelete(task);
                }
            });

            itemView.setOnClickListener(v -> {
                if (longPressListener != null) {
                    longPressListener.onTaskPress(task);
                }
            });
        }
    }
}
