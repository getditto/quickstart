package com.example.dittotasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private List<Task> todos = new ArrayList<>();
    private OnTodoClickListener listener;

    public interface OnTodoClickListener {
        void onTodoChecked(Task todo, boolean isChecked);
    }

    public void setOnTodoClickListener(OnTodoClickListener listener) {
        this.listener = listener;
    }

    public void setTodos(List<Task> todos) {
        this.todos = todos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.todo_item, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Task todo = todos.get(position);
        holder.bind(todo);
    }

    @Override
    public int getItemCount() {
        return todos.size();
    }

    class TodoViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView textView;

        TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.todo_checkbox);
            textView = itemView.findViewById(R.id.todo_text);
        }

        void bind(final Task todo) {
            textView.setText(todo.getTitle());
            checkBox.setChecked(todo.isDone());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTodoChecked(todo, isChecked);
                }
            });
        }
    }
}
