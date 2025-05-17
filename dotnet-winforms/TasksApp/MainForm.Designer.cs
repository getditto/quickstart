namespace DittoTasksApp
{
    partial class MainForm
    {



        /// <summary>
        ///  Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        ///  Clean up any resources being used.
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
        ///  Required method for Designer support - do not modify
        ///  the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(MainForm));
            statusStrip1 = new StatusStrip();
            tsslAppId = new ToolStripStatusLabel();
            tsslAuthToken = new ToolStripStatusLabel();
            menuStrip1 = new MenuStrip();
            fileToolStripMenuItem = new ToolStripMenuItem();
            newToolStripMenuItem = new ToolStripMenuItem();
            toolStripSeparator2 = new ToolStripSeparator();
            exitToolStripMenuItem = new ToolStripMenuItem();
            editToolStripMenuItem = new ToolStripMenuItem();
            undoToolStripMenuItem = new ToolStripMenuItem();
            redoToolStripMenuItem = new ToolStripMenuItem();
            toolStripSeparator3 = new ToolStripSeparator();
            cutToolStripMenuItem = new ToolStripMenuItem();
            copyToolStripMenuItem = new ToolStripMenuItem();
            pasteToolStripMenuItem = new ToolStripMenuItem();
            toolStripSeparator4 = new ToolStripSeparator();
            selectAllToolStripMenuItem = new ToolStripMenuItem();
            helpToolStripMenuItem = new ToolStripMenuItem();
            aboutToolStripMenuItem = new ToolStripMenuItem();
            toolStrip1 = new ToolStrip();
            tsbAdd = new ToolStripButton();
            tsbComplete = new ToolStripButton();
            tsbEdit = new ToolStripButton();
            tsbDelete = new ToolStripButton();
            toolStripLabel1 = new ToolStripLabel();
            tsbSyncStatus = new ToolStripButton();
            lvTasks = new ListView();
            chName = new ColumnHeader();
            chIsDone = new ColumnHeader();
            statusStrip1.SuspendLayout();
            menuStrip1.SuspendLayout();
            toolStrip1.SuspendLayout();
            SuspendLayout();
            // 
            // statusStrip1
            // 
            statusStrip1.Items.AddRange(new ToolStripItem[] { tsslAppId, tsslAuthToken });
            statusStrip1.Location = new Point(0, 707);
            statusStrip1.Name = "statusStrip1";
            statusStrip1.Size = new Size(1008, 22);
            statusStrip1.TabIndex = 0;
            statusStrip1.Text = "statusStrip1";
            // 
            // tsslAppId
            // 
            tsslAppId.Name = "tsslAppId";
            tsslAppId.Padding = new Padding(0, 0, 40, 0);
            tsslAppId.Size = new Size(82, 17);
            tsslAppId.Text = "AppId:";
            // 
            // tsslAuthToken
            // 
            tsslAuthToken.Name = "tsslAuthToken";
            tsslAuthToken.Padding = new Padding(20, 0, 20, 0);
            tsslAuthToken.Size = new Size(111, 17);
            tsslAuthToken.Text = "Auth Token:";
            // 
            // menuStrip1
            // 
            menuStrip1.Items.AddRange(new ToolStripItem[] { fileToolStripMenuItem, editToolStripMenuItem, helpToolStripMenuItem });
            menuStrip1.Location = new Point(0, 0);
            menuStrip1.Name = "menuStrip1";
            menuStrip1.Size = new Size(1008, 24);
            menuStrip1.TabIndex = 1;
            menuStrip1.Text = "menuStrip1";
            // 
            // fileToolStripMenuItem
            // 
            fileToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { newToolStripMenuItem, toolStripSeparator2, exitToolStripMenuItem });
            fileToolStripMenuItem.Name = "fileToolStripMenuItem";
            fileToolStripMenuItem.Size = new Size(37, 20);
            fileToolStripMenuItem.Text = "&File";
            // 
            // newToolStripMenuItem
            // 
            newToolStripMenuItem.Image = (Image)resources.GetObject("newToolStripMenuItem.Image");
            newToolStripMenuItem.ImageTransparentColor = Color.Magenta;
            newToolStripMenuItem.Name = "newToolStripMenuItem";
            newToolStripMenuItem.ShortcutKeys = Keys.Control | Keys.N;
            newToolStripMenuItem.Size = new Size(141, 22);
            newToolStripMenuItem.Text = "&New";
            newToolStripMenuItem.Click += newToolStripMenuItem_Click;
            // 
            // toolStripSeparator2
            // 
            toolStripSeparator2.Name = "toolStripSeparator2";
            toolStripSeparator2.Size = new Size(138, 6);
            // 
            // exitToolStripMenuItem
            // 
            exitToolStripMenuItem.Name = "exitToolStripMenuItem";
            exitToolStripMenuItem.Size = new Size(141, 22);
            exitToolStripMenuItem.Text = "E&xit";
            exitToolStripMenuItem.Click += exitToolStripMenuItem_Click;
            // 
            // editToolStripMenuItem
            // 
            editToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { undoToolStripMenuItem, redoToolStripMenuItem, toolStripSeparator3, cutToolStripMenuItem, copyToolStripMenuItem, pasteToolStripMenuItem, toolStripSeparator4, selectAllToolStripMenuItem });
            editToolStripMenuItem.Name = "editToolStripMenuItem";
            editToolStripMenuItem.Size = new Size(39, 20);
            editToolStripMenuItem.Text = "&Edit";
            // 
            // undoToolStripMenuItem
            // 
            undoToolStripMenuItem.Name = "undoToolStripMenuItem";
            undoToolStripMenuItem.ShortcutKeys = Keys.Control | Keys.Z;
            undoToolStripMenuItem.Size = new Size(144, 22);
            undoToolStripMenuItem.Text = "&Undo";
            // 
            // redoToolStripMenuItem
            // 
            redoToolStripMenuItem.Name = "redoToolStripMenuItem";
            redoToolStripMenuItem.ShortcutKeys = Keys.Control | Keys.Y;
            redoToolStripMenuItem.Size = new Size(144, 22);
            redoToolStripMenuItem.Text = "&Redo";
            // 
            // toolStripSeparator3
            // 
            toolStripSeparator3.Name = "toolStripSeparator3";
            toolStripSeparator3.Size = new Size(141, 6);
            // 
            // cutToolStripMenuItem
            // 
            cutToolStripMenuItem.Image = (Image)resources.GetObject("cutToolStripMenuItem.Image");
            cutToolStripMenuItem.ImageTransparentColor = Color.Magenta;
            cutToolStripMenuItem.Name = "cutToolStripMenuItem";
            cutToolStripMenuItem.ShortcutKeys = Keys.Control | Keys.X;
            cutToolStripMenuItem.Size = new Size(144, 22);
            cutToolStripMenuItem.Text = "Cu&t";
            // 
            // copyToolStripMenuItem
            // 
            copyToolStripMenuItem.Image = (Image)resources.GetObject("copyToolStripMenuItem.Image");
            copyToolStripMenuItem.ImageTransparentColor = Color.Magenta;
            copyToolStripMenuItem.Name = "copyToolStripMenuItem";
            copyToolStripMenuItem.ShortcutKeys = Keys.Control | Keys.C;
            copyToolStripMenuItem.Size = new Size(144, 22);
            copyToolStripMenuItem.Text = "&Copy";
            // 
            // pasteToolStripMenuItem
            // 
            pasteToolStripMenuItem.Image = (Image)resources.GetObject("pasteToolStripMenuItem.Image");
            pasteToolStripMenuItem.ImageTransparentColor = Color.Magenta;
            pasteToolStripMenuItem.Name = "pasteToolStripMenuItem";
            pasteToolStripMenuItem.ShortcutKeys = Keys.Control | Keys.V;
            pasteToolStripMenuItem.Size = new Size(144, 22);
            pasteToolStripMenuItem.Text = "&Paste";
            // 
            // toolStripSeparator4
            // 
            toolStripSeparator4.Name = "toolStripSeparator4";
            toolStripSeparator4.Size = new Size(141, 6);
            // 
            // selectAllToolStripMenuItem
            // 
            selectAllToolStripMenuItem.Name = "selectAllToolStripMenuItem";
            selectAllToolStripMenuItem.Size = new Size(144, 22);
            selectAllToolStripMenuItem.Text = "Select &All";
            // 
            // helpToolStripMenuItem
            // 
            helpToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { aboutToolStripMenuItem });
            helpToolStripMenuItem.Name = "helpToolStripMenuItem";
            helpToolStripMenuItem.Size = new Size(44, 20);
            helpToolStripMenuItem.Text = "&Help";
            // 
            // aboutToolStripMenuItem
            // 
            aboutToolStripMenuItem.Name = "aboutToolStripMenuItem";
            aboutToolStripMenuItem.Size = new Size(116, 22);
            aboutToolStripMenuItem.Text = "&About...";
            aboutToolStripMenuItem.Click += aboutToolStripMenuItem_Click;
            // 
            // toolStrip1
            // 
            toolStrip1.Items.AddRange(new ToolStripItem[] { tsbAdd, tsbComplete, tsbEdit, tsbDelete, toolStripLabel1, tsbSyncStatus });
            toolStrip1.Location = new Point(0, 24);
            toolStrip1.Name = "toolStrip1";
            toolStrip1.Size = new Size(1008, 25);
            toolStrip1.TabIndex = 2;
            toolStrip1.Text = "toolStrip1";
            // 
            // tsbAdd
            // 
            tsbAdd.DisplayStyle = ToolStripItemDisplayStyle.Image;
            tsbAdd.Image = (Image)resources.GetObject("tsbAdd.Image");
            tsbAdd.ImageTransparentColor = Color.Magenta;
            tsbAdd.Name = "tsbAdd";
            tsbAdd.Padding = new Padding(0, 0, 20, 0);
            tsbAdd.Size = new Size(40, 22);
            tsbAdd.Text = "New";
            tsbAdd.Click += tsbAdd_Click;
            // 
            // tsbComplete
            // 
            tsbComplete.DisplayStyle = ToolStripItemDisplayStyle.Image;
            tsbComplete.Enabled = false;
            tsbComplete.Image = (Image)resources.GetObject("tsbComplete.Image");
            tsbComplete.ImageTransparentColor = Color.Magenta;
            tsbComplete.Name = "tsbComplete";
            tsbComplete.Padding = new Padding(0, 0, 20, 0);
            tsbComplete.Size = new Size(40, 22);
            tsbComplete.Text = "Update";
            tsbComplete.Click += tsbComplete_Click;
            // 
            // tsbEdit
            // 
            tsbEdit.DisplayStyle = ToolStripItemDisplayStyle.Image;
            tsbEdit.Enabled = false;
            tsbEdit.Image = (Image)resources.GetObject("tsbEdit.Image");
            tsbEdit.ImageTransparentColor = Color.Magenta;
            tsbEdit.Name = "tsbEdit";
            tsbEdit.Padding = new Padding(0, 0, 20, 0);
            tsbEdit.Size = new Size(40, 22);
            tsbEdit.Text = "Edit";
            tsbEdit.TextAlign = ContentAlignment.TopRight;
            tsbEdit.ToolTipText = "Edit";
            tsbEdit.Click += tsbEdit_Click;
            // 
            // tsbDelete
            // 
            tsbDelete.DisplayStyle = ToolStripItemDisplayStyle.Image;
            tsbDelete.Enabled = false;
            tsbDelete.Image = (Image)resources.GetObject("tsbDelete.Image");
            tsbDelete.ImageTransparentColor = Color.Magenta;
            tsbDelete.Name = "tsbDelete";
            tsbDelete.Padding = new Padding(0, 0, 20, 0);
            tsbDelete.Size = new Size(40, 22);
            tsbDelete.Text = "Delete";
            tsbDelete.Click += tsbDelete_Click;
            // 
            // toolStripLabel1
            // 
            toolStripLabel1.Margin = new Padding(40, 1, 0, 2);
            toolStripLabel1.Name = "toolStripLabel1";
            toolStripLabel1.Size = new Size(35, 22);
            toolStripLabel1.Text = "Sync:";
            // 
            // tsbSyncStatus
            // 
            tsbSyncStatus.DisplayStyle = ToolStripItemDisplayStyle.Image;
            tsbSyncStatus.Image = (Image)resources.GetObject("tsbSyncStatus.Image");
            tsbSyncStatus.ImageTransparentColor = Color.Magenta;
            tsbSyncStatus.Name = "tsbSyncStatus";
            tsbSyncStatus.Size = new Size(23, 22);
            tsbSyncStatus.Text = "Sync";
            tsbSyncStatus.Click += tsbSyncStatus_Click;
            // 
            // lvTasks
            // 
            lvTasks.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lvTasks.CheckBoxes = true;
            lvTasks.Columns.AddRange(new ColumnHeader[] { chName, chIsDone });
            lvTasks.GridLines = true;
            lvTasks.Location = new Point(12, 52);
            lvTasks.MultiSelect = false;
            lvTasks.Name = "lvTasks";
            lvTasks.Size = new Size(984, 652);
            lvTasks.TabIndex = 3;
            lvTasks.UseCompatibleStateImageBehavior = false;
            lvTasks.View = View.Details;
            lvTasks.ItemCheck += lvTasks_ItemCheck;
            lvTasks.ItemChecked += lvTasks_ItemChecked;
            // 
            // chName
            // 
            chName.Tag = "Name";
            chName.Text = "Task";
            chName.Width = 900;
            // 
            // chIsDone
            // 
            chIsDone.Tag = "IsDone";
            chIsDone.Text = "Done";
            chIsDone.TextAlign = HorizontalAlignment.Center;
            chIsDone.Width = 100;
            // 
            // MainForm
            // 
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(1008, 729);
            Controls.Add(lvTasks);
            Controls.Add(toolStrip1);
            Controls.Add(statusStrip1);
            Controls.Add(menuStrip1);
            MainMenuStrip = menuStrip1;
            Name = "MainForm";
            Text = "Ditto Task App";
            statusStrip1.ResumeLayout(false);
            statusStrip1.PerformLayout();
            menuStrip1.ResumeLayout(false);
            menuStrip1.PerformLayout();
            toolStrip1.ResumeLayout(false);
            toolStrip1.PerformLayout();
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private StatusStrip statusStrip1;
        private ToolStripStatusLabel tsslAppId;
        private ToolStripStatusLabel tsslAuthToken;
        private MenuStrip menuStrip1;
        private ToolStripMenuItem fileToolStripMenuItem;
        private ToolStripMenuItem newToolStripMenuItem;
        private ToolStripSeparator toolStripSeparator2;
        private ToolStripMenuItem exitToolStripMenuItem;
        private ToolStripMenuItem editToolStripMenuItem;
        private ToolStripMenuItem undoToolStripMenuItem;
        private ToolStripMenuItem redoToolStripMenuItem;
        private ToolStripSeparator toolStripSeparator3;
        private ToolStripMenuItem cutToolStripMenuItem;
        private ToolStripMenuItem copyToolStripMenuItem;
        private ToolStripMenuItem pasteToolStripMenuItem;
        private ToolStripSeparator toolStripSeparator4;
        private ToolStripMenuItem selectAllToolStripMenuItem;
        private ToolStripMenuItem helpToolStripMenuItem;
        private ToolStripMenuItem aboutToolStripMenuItem;
        private ToolStrip toolStrip1;
        private ToolStripButton tsbAdd;
        private ToolStripButton tsbComplete;
        private ToolStripButton tsbDelete;
        private ToolStripLabel toolStripLabel1;
        private ToolStripButton tsbSyncStatus;
        private ListView lvTasks;
        private ColumnHeader chName;
        private ColumnHeader chIsDone;
        private ToolStripButton tsbEdit;
    }
}
