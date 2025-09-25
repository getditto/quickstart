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
    public partial class ToDoTaskEditorForm : Form
    {
        private readonly TasksPeer _tasksPeer;
        private readonly ToDoTask? _task;
        public ToDoTaskEditorForm(TasksPeer tasksPeer, ToDoTask? task)
        {
            _task = task;
            _tasksPeer = tasksPeer;

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
                await _tasksPeer.AddTask(tbName.Text);
                CloseForm();
            }
            else
            {
                await _tasksPeer.UpdateTaskTitle(_task.Id, tbName.Text);
                await _tasksPeer.UpdateTaskDone(_task.Id, cbIsCompleted.Checked);
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
