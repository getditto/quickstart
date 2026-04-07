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
            lblNameTxt = new Label();
            tbName = new TextBox();
            lblIsCompleteTxt = new Label();
            cbIsCompleted = new CheckBox();
            btnSave2 = new Button();
            btnCancel2 = new Button();
            SuspendLayout();
            // 
            // lblNameTxt
            // 
            lblNameTxt.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            lblNameTxt.AutoSize = true;
            lblNameTxt.Location = new Point(29, 126);
            lblNameTxt.Margin = new Padding(7, 0, 7, 0);
            lblNameTxt.Name = "lblNameTxt";
            lblNameTxt.Size = new Size(74, 41);
            lblNameTxt.TabIndex = 2;
            lblNameTxt.Text = "Task";
            // 
            // tbName
            // 
            tbName.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            tbName.Location = new Point(138, 118);
            tbName.Margin = new Padding(7, 8, 7, 8);
            tbName.Multiline = true;
            tbName.Name = "tbName";
            tbName.Size = new Size(978, 176);
            tbName.TabIndex = 3;
            // 
            // lblIsCompleteTxt
            // 
            lblIsCompleteTxt.AutoSize = true;
            lblIsCompleteTxt.Location = new Point(29, 361);
            lblIsCompleteTxt.Margin = new Padding(7, 0, 7, 0);
            lblIsCompleteTxt.Name = "lblIsCompleteTxt";
            lblIsCompleteTxt.Size = new Size(179, 41);
            lblIsCompleteTxt.TabIndex = 4;
            lblIsCompleteTxt.Text = "Is Completd";
            // 
            // cbIsCompleted
            // 
            cbIsCompleted.AutoSize = true;
            cbIsCompleted.Location = new Point(216, 364);
            cbIsCompleted.Margin = new Padding(7, 8, 7, 8);
            cbIsCompleted.Name = "cbIsCompleted";
            cbIsCompleted.Size = new Size(34, 33);
            cbIsCompleted.TabIndex = 5;
            cbIsCompleted.UseVisualStyleBackColor = true;
            // 
            // btnSave2
            // 
            btnSave2.Location = new Point(682, 352);
            btnSave2.Name = "btnSave2";
            btnSave2.Size = new Size(188, 58);
            btnSave2.TabIndex = 6;
            btnSave2.Text = "Save";
            btnSave2.UseVisualStyleBackColor = true;
            btnSave2.Click += btnSave_Click;
            // 
            // btnCancel2
            // 
            btnCancel2.Location = new Point(904, 352);
            btnCancel2.Name = "btnCancel2";
            btnCancel2.Size = new Size(188, 58);
            btnCancel2.TabIndex = 7;
            btnCancel2.Text = "Cancel";
            btnCancel2.UseVisualStyleBackColor = true;
            btnCancel2.Click += btnCancel_Click;
            // 
            // ToDoTaskEditorForm
            // 
            AutoScaleDimensions = new SizeF(17F, 41F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(1137, 443);
            Controls.Add(btnCancel2);
            Controls.Add(btnSave2);
            Controls.Add(cbIsCompleted);
            Controls.Add(lblIsCompleteTxt);
            Controls.Add(tbName);
            Controls.Add(lblNameTxt);
            Margin = new Padding(7, 8, 7, 8);
            MaximumSize = new Size(1169, 531);
            MinimumSize = new Size(1169, 531);
            Name = "ToDoTaskEditorForm";
            StartPosition = FormStartPosition.CenterParent;
            Text = "ToDo Task Editor";
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private Label lblNameTxt;
        private TextBox tbName;
        private Label lblIsCompleteTxt;
        private CheckBox cbIsCompleted;
        private Button btnSave2;
        private Button btnCancel2;
    }
}