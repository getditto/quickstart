package main

import (
	"context"

	"github.com/getditto/ditto-go-sdk/v5/ditto"
	"github.com/google/uuid"
)

// Task is a single todo item as stored in the "tasks" collection.
type Task struct {
	ID      string `json:"_id"`
	Title   string `json:"title"`
	Done    bool   `json:"done"`
	Deleted bool   `json:"deleted"`
}

// TasksRepository owns everything specific to the tasks data: the sync
// subscription, the store observer that streams the current task list, and the
// task CRUD operations. It talks to Ditto directly through the instance vended
// by DittoManager.
type TasksRepository struct {
	ditto        *ditto.Ditto
	subscription *ditto.SyncSubscription
	observer     *ditto.StoreObserver
}

// NewTasksRepository creates a repository backed by the manager's Ditto
// instance.
func NewTasksRepository(manager *DittoManager) *TasksRepository {
	return &TasksRepository{ditto: manager.Ditto()}
}

// RegisterSubscription subscribes this peer to the tasks collection so Ditto
// syncs matching documents from other peers.
func (r *TasksRepository) RegisterSubscription() error {
	subscription, err := r.ditto.Sync().RegisterSubscription("SELECT * FROM tasks")
	if err != nil {
		return err
	}
	r.subscription = subscription
	return nil
}

// ObserveTasks registers a store observer against the local database and
// returns a channel that receives the current task list whenever it changes.
func (r *TasksRepository) ObserveTasks(ctx context.Context) (<-chan []Task, error) {
	tasksChan := make(chan []Task, 1) // Buffer size 1 - latest update wins

	observer, err := r.ditto.Store().RegisterObserver(
		"SELECT * FROM tasks WHERE NOT deleted",
		nil,
		func(result *ditto.QueryResult) {
			defer result.Close()

			tasks := parseTasks(result)
			select {
			case tasksChan <- tasks:
			case <-ctx.Done():
			}
		})
	if err != nil {
		return nil, err
	}
	r.observer = observer
	return tasksChan, nil
}

func (r *TasksRepository) CreateTask(title string) error {
	task := map[string]interface{}{
		"_id":     uuid.New().String(),
		"title":   title,
		"done":    false,
		"deleted": false,
	}

	result, err := r.ditto.Store().Execute(
		"INSERT INTO tasks DOCUMENTS (:task)",
		ditto.QueryArguments{"task": task},
	)
	if err != nil {
		return err
	}
	result.Close()
	return nil
}

func (r *TasksRepository) UpdateTask(id, title string) error {
	result, err := r.ditto.Store().Execute(
		"UPDATE tasks SET title = :title WHERE _id = :id",
		ditto.QueryArguments{
			"title": title,
			"id":    id,
		},
	)
	if err != nil {
		return err
	}
	result.Close()
	return nil
}

func (r *TasksRepository) ToggleTask(id string, done bool) error {
	result, err := r.ditto.Store().Execute(
		"UPDATE tasks SET done = :done WHERE _id = :id",
		ditto.QueryArguments{
			"done": done,
			"id":   id,
		},
	)
	if err != nil {
		return err
	}
	result.Close()
	return nil
}

func (r *TasksRepository) DeleteTask(id string) error {
	result, err := r.ditto.Store().Execute(
		"UPDATE tasks SET deleted = true WHERE _id = :id",
		ditto.QueryArguments{"id": id},
	)
	if err != nil {
		return err
	}
	result.Close()
	return nil
}

// Close cancels the observer and subscription held by the repository.
func (r *TasksRepository) Close() {
	if r.observer != nil {
		r.observer.Cancel()
	}
	if r.subscription != nil {
		r.subscription.Cancel()
	}
}

func parseTasks(result *ditto.QueryResult) []Task {
	if result == nil {
		return []Task{}
	}

	tasks := make([]Task, 0, result.ItemCount())
	for _, queryItem := range result.Items() {
		var task Task
		if err := queryItem.UnmarshalTo(&task); err != nil {
			panic(err)
		}
		tasks = append(tasks, task)
	}
	return tasks
}
