namespace Taskapp.WinForms.Net48
{
    partial class MainForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.tasksListView = new System.Windows.Forms.ListView();
            this.doneColumn = ((System.Windows.Forms.ColumnHeader)(new System.Windows.Forms.ColumnHeader()));
            this.titleColumn = ((System.Windows.Forms.ColumnHeader)(new System.Windows.Forms.ColumnHeader()));
            this.idColumn = ((System.Windows.Forms.ColumnHeader)(new System.Windows.Forms.ColumnHeader()));
            this.newTaskTextBox = new System.Windows.Forms.TextBox();
            this.addTaskButton = new System.Windows.Forms.Button();
            this.editTaskButton = new System.Windows.Forms.Button();
            this.deleteTaskButton = new System.Windows.Forms.Button();
            this.statusLabel = new System.Windows.Forms.Label();
            this.SuspendLayout();
            //
            // tasksListView
            //
            this.tasksListView.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
            | System.Windows.Forms.AnchorStyles.Left)
            | System.Windows.Forms.AnchorStyles.Right)));
            this.tasksListView.CheckBoxes = true;
            this.tasksListView.Columns.AddRange(new System.Windows.Forms.ColumnHeader[] {
            this.doneColumn,
            this.titleColumn,
            this.idColumn});
            this.tasksListView.FullRowSelect = true;
            this.tasksListView.GridLines = true;
            this.tasksListView.HideSelection = false;
            this.tasksListView.Location = new System.Drawing.Point(12, 12);
            this.tasksListView.Name = "tasksListView";
            this.tasksListView.Size = new System.Drawing.Size(760, 350);
            this.tasksListView.TabIndex = 0;
            this.tasksListView.UseCompatibleStateImageBehavior = false;
            this.tasksListView.View = System.Windows.Forms.View.Details;
            this.tasksListView.ItemCheck += new System.Windows.Forms.ItemCheckEventHandler(this.tasksListView_ItemCheck);
            //
            // doneColumn
            //
            this.doneColumn.Text = "Done";
            this.doneColumn.Width = 50;
            //
            // titleColumn
            //
            this.titleColumn.Text = "Title";
            this.titleColumn.Width = 500;
            //
            // idColumn
            //
            this.idColumn.Text = "ID";
            this.idColumn.Width = 0;
            //
            // newTaskTextBox
            //
            this.newTaskTextBox.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
            | System.Windows.Forms.AnchorStyles.Right)));
            this.newTaskTextBox.Location = new System.Drawing.Point(12, 378);
            this.newTaskTextBox.Name = "newTaskTextBox";
            this.newTaskTextBox.Size = new System.Drawing.Size(540, 20);
            this.newTaskTextBox.TabIndex = 1;
            this.newTaskTextBox.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.newTaskTextBox_KeyPress);
            //
            // addTaskButton
            //
            this.addTaskButton.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.addTaskButton.Location = new System.Drawing.Point(558, 376);
            this.addTaskButton.Name = "addTaskButton";
            this.addTaskButton.Size = new System.Drawing.Size(100, 23);
            this.addTaskButton.TabIndex = 2;
            this.addTaskButton.Text = "Add Task";
            this.addTaskButton.UseVisualStyleBackColor = true;
            this.addTaskButton.Click += new System.EventHandler(this.addTaskButton_Click);
            //
            // editTaskButton
            //
            this.editTaskButton.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.editTaskButton.Location = new System.Drawing.Point(664, 376);
            this.editTaskButton.Name = "editTaskButton";
            this.editTaskButton.Size = new System.Drawing.Size(100, 23);
            this.editTaskButton.TabIndex = 3;
            this.editTaskButton.Text = "Edit Task";
            this.editTaskButton.UseVisualStyleBackColor = true;
            this.editTaskButton.Click += new System.EventHandler(this.editTaskButton_Click);
            //
            // deleteTaskButton
            //
            this.deleteTaskButton.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.deleteTaskButton.Location = new System.Drawing.Point(664, 405);
            this.deleteTaskButton.Name = "deleteTaskButton";
            this.deleteTaskButton.Size = new System.Drawing.Size(100, 23);
            this.deleteTaskButton.TabIndex = 4;
            this.deleteTaskButton.Text = "Delete Task";
            this.deleteTaskButton.UseVisualStyleBackColor = true;
            this.deleteTaskButton.Click += new System.EventHandler(this.deleteTaskButton_Click);
            //
            // statusLabel
            //
            this.statusLabel.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.statusLabel.AutoSize = true;
            this.statusLabel.Location = new System.Drawing.Point(12, 410);
            this.statusLabel.Name = "statusLabel";
            this.statusLabel.Size = new System.Drawing.Size(79, 13);
            this.statusLabel.TabIndex = 5;
            this.statusLabel.Text = "Status: Ready";
            //
            // MainForm
            //
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(784, 441);
            this.Controls.Add(this.statusLabel);
            this.Controls.Add(this.deleteTaskButton);
            this.Controls.Add(this.editTaskButton);
            this.Controls.Add(this.addTaskButton);
            this.Controls.Add(this.newTaskTextBox);
            this.Controls.Add(this.tasksListView);
            this.Name = "MainForm";
            this.Text = "Tasks";
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.MainForm_FormClosing);
            this.Load += new System.EventHandler(this.MainForm_Load);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.ListView tasksListView;
        private System.Windows.Forms.ColumnHeader doneColumn;
        private System.Windows.Forms.ColumnHeader titleColumn;
        private System.Windows.Forms.ColumnHeader idColumn;
        private System.Windows.Forms.TextBox newTaskTextBox;
        private System.Windows.Forms.Button addTaskButton;
        private System.Windows.Forms.Button editTaskButton;
        private System.Windows.Forms.Button deleteTaskButton;
        private System.Windows.Forms.Label statusLabel;
    }
}

