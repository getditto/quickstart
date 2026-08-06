using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace DittoTasksApp
{
    public partial class TaskEditorForm : Form
    {
        private readonly TasksRepository _tasksRepository;
        private readonly TaskModel? _task;
        public TaskEditorForm(TasksRepository tasksRepository, TaskModel? task)
        {
            _task = task;
            _tasksRepository = tasksRepository;

            InitializeComponent();
            SetFormValues();
        }

        private void SetFormValues()
        {
            if (_task != null)
            {
                tbName.Text = _task.Title;
                cbIsCompleted.Checked = _task.Done;
            }
            else
            {
                cbIsCompleted.Visible = false;
                lblIsCompleteTxt.Visible = false;
            }
        }

        private async void btnSave_Click(object sender, EventArgs e)
        {
            if (_task == null)
            {
                await _tasksRepository.AddTask(tbName.Text);
                CloseForm();
            }
            else
            {
                await _tasksRepository.UpdateTaskTitle(_task.Id, tbName.Text);
                await _tasksRepository.UpdateTaskDone(_task.Id, cbIsCompleted.Checked);
                CloseForm();
            }

        }

        private void btnCancel_Click(object sender, EventArgs e) => CloseForm();

        private void CloseForm()
        {
            this.Close();
            this.Dispose();
        }
    }
}
