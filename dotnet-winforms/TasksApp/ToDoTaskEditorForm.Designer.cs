namespace DittoTasksApp
{
    partial class ToDoTaskEditorForm
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
            btnSave = new Button();
            btnCancel = new Button();
            lblNameTxt = new Label();
            tbName = new TextBox();
            lblIsCompleteTxt = new Label();
            cbIsCompleted = new CheckBox();
            SuspendLayout();
            // 
            // btnSave
            // 
            btnSave.Location = new Point(153, 176);
            btnSave.Name = "btnSave";
            btnSave.Size = new Size(75, 23);
            btnSave.TabIndex = 0;
            btnSave.Text = "Save";
            btnSave.UseVisualStyleBackColor = true;
            btnSave.Click += btnSave_Click;
            // 
            // btnCancel
            // 
            btnCancel.Location = new Point(245, 176);
            btnCancel.Name = "btnCancel";
            btnCancel.Size = new Size(75, 23);
            btnCancel.TabIndex = 1;
            btnCancel.Text = "Cancel";
            btnCancel.UseVisualStyleBackColor = true;
            btnCancel.Click += btnCancel_Click;
            // 
            // lblNameTxt
            // 
            lblNameTxt.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            lblNameTxt.AutoSize = true;
            lblNameTxt.Location = new Point(12, 46);
            lblNameTxt.Name = "lblNameTxt";
            lblNameTxt.Size = new Size(30, 15);
            lblNameTxt.TabIndex = 2;
            lblNameTxt.Text = "Task";
            // 
            // tbName
            // 
            tbName.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            tbName.Location = new Point(57, 43);
            tbName.Multiline = true;
            tbName.Name = "tbName";
            tbName.Size = new Size(405, 67);
            tbName.TabIndex = 3;
            // 
            // lblIsCompleteTxt
            // 
            lblIsCompleteTxt.AutoSize = true;
            lblIsCompleteTxt.Location = new Point(12, 132);
            lblIsCompleteTxt.Name = "lblIsCompleteTxt";
            lblIsCompleteTxt.Size = new Size(71, 15);
            lblIsCompleteTxt.TabIndex = 4;
            lblIsCompleteTxt.Text = "Is Completd";
            // 
            // cbIsCompleted
            // 
            cbIsCompleted.AutoSize = true;
            cbIsCompleted.Location = new Point(89, 133);
            cbIsCompleted.Name = "cbIsCompleted";
            cbIsCompleted.Size = new Size(15, 14);
            cbIsCompleted.TabIndex = 5;
            cbIsCompleted.UseVisualStyleBackColor = true;
            // 
            // ToDoTaskEditorForm
            // 
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(484, 211);
            Controls.Add(cbIsCompleted);
            Controls.Add(lblIsCompleteTxt);
            Controls.Add(tbName);
            Controls.Add(lblNameTxt);
            Controls.Add(btnCancel);
            Controls.Add(btnSave);
            MaximumSize = new Size(500, 250);
            MinimumSize = new Size(500, 250);
            Name = "ToDoTaskEditorForm";
            StartPosition = FormStartPosition.CenterParent;
            Text = "ToDo Task Editor";
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private Button btnSave;
        private Button btnCancel;
        private Label lblNameTxt;
        private TextBox tbName;
        private Label lblIsCompleteTxt;
        private CheckBox cbIsCompleted;
    }
}