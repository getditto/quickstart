using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;

using DittoSDK;
using Terminal.Gui;
using NStack;

public class TasksWindow : Window
{
    /// <summary>
    /// Terminal.Gui data source adapter for the tasks list retrieved from Ditto.
    /// </summary>
    class TasksListDataSource : IListDataSource
    {
        // TODO: Does this need to be ObservableCollection or will a List suffice?
        private ObservableCollection<DittoTask> _tasks;

        public TasksListDataSource(ObservableCollection<DittoTask> tasks)
        {
            _tasks = tasks;
        }

        public int Count => _tasks.Count;

        public bool IsMarked(int item)
        {
            if (item < 0 || item >= _tasks.Count)
            {
                return false;
            }
            return _tasks[item].Done;
        }

        public int Length
        {
            get
            {
                int maxLength = 0;
                foreach (var task in _tasks)
                {
                    maxLength = Math.Max(maxLength, task.Title.Length);
                }
                return maxLength;
            }
        }

        public void SetMark(int item, bool value)
        {
            if (item < 0 || item >= _tasks.Count)
            {
                return;
            }

            _tasks[item].Done = value;
        }

        public void Render(ListView container, ConsoleDriver driver, bool selected, int item, int col, int line, int width, int start = 0)
        {
            if (item >= _tasks.Count)
            {
                return;
            }

            var task = _tasks[item];
            var text = task.Title ?? "<null>";

            var savedClip = container.ClipToBounds();
            container.Move(col, line);
            RenderUstr(driver, text, col, line, width, start);
            driver.Clip = savedClip;
        }

        void RenderUstr(ConsoleDriver driver, ustring ustr, int col, int line, int width, int start = 0)
        {
            ustring str = start > ustr.ConsoleWidth ? string.Empty : ustr.Substring(Math.Min(start, ustr.ToRunes().Length - 1));
            ustring u = TextFormatter.ClipAndJustify(str, width, TextAlignment.Left);
            driver.AddStr(u);
            width -= TextFormatter.GetTextWidth(u);
            while (width-- + start > 0)
            {
                driver.AddRune(' ');
            }
        }

        public IList ToList()
        {
            return _tasks;
        }

        public void UpdateTasks(IList<DittoTask> newTasks)
        {
            var oldCount = _tasks.Count;
            var newCount = newTasks.Count;
            var count = Math.Min(oldCount, newCount);

            for (int i = 0; i < count; i++)
            {
                _tasks[i] = newTasks[i];
            }

            if (newCount > oldCount)
            {
                for (int i = oldCount; i < newCount; i++)
                {
                    _tasks.Add(newTasks[i]);
                }
            }
            else if (newCount < oldCount)
            {
                for (int i = oldCount - 1; i >= newCount; i--)
                {
                    _tasks.RemoveAt(i);
                }
            }
        }
    }

    private readonly TasksPeer _peer;
    private TasksListDataSource _dataSource;
    private DittoStoreObserver _observer;

    public TasksWindow(TasksPeer peer) : base($"Ditto Tasks - {Application.QuitKey} to exit")
    {
        this._peer = peer;
        _dataSource = new TasksListDataSource(new());

        X = 0;
        Y = 1;
        Width = Dim.Fill();
        Height = Dim.Fill();

        var tasksListView = new ListView
        {
            X = 0,
            Y = 1,
            Width = Dim.Fill(),
            Height = Dim.Fill(),
            Source = _dataSource,
            AllowsMarking = true,
        };
        Add(tasksListView);

        _observer = peer.ObserveTasksCollection(async (tasks) =>
        {
            await Task.Run(() =>
            {
                Application.MainLoop.Invoke(() =>
                {
                    _dataSource.UpdateTasks(tasks);
                    tasksListView.SetNeedsDisplay();
                });
            });
        });
    }

    public override bool ProcessKey(KeyEvent keyEvent)
    {
        if (keyEvent.Key == Key.Q)
        {
            Application.RequestStop();
            return true;
        }
        return base.ProcessKey(keyEvent);
    }

    public override bool OnKeyDown(KeyEvent keyEvent)
    {
        if (keyEvent.Key == Key.Q)
        {
            Application.RequestStop();
            return true;
        }
        return base.OnKeyDown(keyEvent);
    }

    public override bool OnKeyUp(KeyEvent keyEvent)
    {
        if (keyEvent.Key == Key.Q)
        {
            Application.RequestStop();
            return true;
        }
        return base.OnKeyUp(keyEvent);
    }
}