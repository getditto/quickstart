using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;

using DittoSDK;

using Terminal.Gui;
using Terminal.Gui.Graphs;
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
            get => _tasks.Max(t => t.Title?.Length ?? 0);
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
            // Note: Simply replacing the ObservableCollection with a new one
            // will cause the ListView to lose its selection state.  So we
            // update the existing collection in place.

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

    private readonly DittoTasksPeer _peer;
    private TasksListDataSource _dataSource;
    private DittoStoreObserver _observer;

    public TasksWindow(DittoTasksPeer peer) : base($"Ditto Tasks - {Application.QuitKey} to exit")
    {
        _peer = peer;
        _dataSource = new(new());

        X = 0;
        Y = 0;
        Width = Dim.Fill();
        Height = Dim.Fill();

        // Header panel

        Add(new Label
        {
            Text = "App ID: " + _peer.AppId,
            X = Pos.Center(),
            Y = 0,
        });

        Add(new Label
        {
            Text = "Playground Token: " + _peer.PlaygroundToken,
            X = Pos.Center(),
            Y = 1,
        });

        Add(new LineView(Orientation.Horizontal)
        {
            X = 0,
            Y = 2,
            Width = Dim.Fill(),
        });

        // List panel

        var tasksListView = new ListView
        {
            X = 0,
            Y = 3,
            Width = Dim.Fill(),
            Height = Dim.Fill() - 2,
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

        // Footer panel

        Add(new LineView(Orientation.Horizontal)
        {
            X = 0,
            Y = Pos.Bottom(this) - 4,
            Width = Dim.Fill(),
        });

        Add(new Label
        {
            Text = "(j↑) (k↓) (Space/Enter: toggle) (c: create) (d: delete) (e: edit) (q: quit)",
            X = Pos.Center(),
            Y = Pos.Bottom(this) - 3,
        });
    }
}
