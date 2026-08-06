
using System.Runtime.ConstrainedExecution;

namespace DittoTasksApp
{
    partial class AboutForm
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
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(AboutForm));
            pictureBox1 = new PictureBox();
            llDittoDocs = new LinkLabel();
            lblSDKVersionText = new Label();
            lblSDKVersion = new Label();
            lblDatabaseIdText = new Label();
            lblDatabaseIdValue = new Label();
            lblDevelopmentTokenTxt = new Label();
            lblDevelopmentTokenValue = new Label();
            lblServerUrlTxt = new Label();
            lblServerURLValue = new Label();
            btnClose = new Button();
            ((System.ComponentModel.ISupportInitialize)pictureBox1).BeginInit();
            SuspendLayout();
            //
            // pictureBox1
            //
            pictureBox1.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            pictureBox1.Image = (Image)resources.GetObject("pictureBox1.Image");
            pictureBox1.Location = new Point(12, 12);
            pictureBox1.Name = "pictureBox1";
            pictureBox1.Size = new Size(560, 100);
            pictureBox1.SizeMode = PictureBoxSizeMode.Zoom;
            pictureBox1.TabIndex = 0;
            pictureBox1.TabStop = false;
            //
            // llDittoDocs
            //
            llDittoDocs.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            llDittoDocs.AutoSize = true;
            llDittoDocs.Location = new Point(12, 132);
            llDittoDocs.Name = "llDittoDocs";
            llDittoDocs.Size = new Size(154, 15);
            llDittoDocs.TabIndex = 1;
            llDittoDocs.TabStop = true;
            llDittoDocs.Text = "Ditto Quickstart - Tasks App";
            llDittoDocs.LinkClicked += llDittoDocs_LinkClicked;
            //
            // lblSDKVersionText
            //
            lblSDKVersionText.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            lblSDKVersionText.AutoSize = true;
            lblSDKVersionText.Location = new Point(12, 158);
            lblSDKVersionText.Name = "lblSDKVersionText";
            lblSDKVersionText.Size = new Size(101, 15);
            lblSDKVersionText.TabIndex = 2;
            lblSDKVersionText.Text = "Ditto SDK Version:";
            //
            // lblSDKVersion
            //
            lblSDKVersion.AutoSize = true;
            lblSDKVersion.Location = new Point(126, 158);
            lblSDKVersion.Name = "lblSDKVersion";
            lblSDKVersion.Size = new Size(40, 15);
            lblSDKVersion.TabIndex = 3;
            lblSDKVersion.Text = "0.0.0.0";
            //
            // lblDatabaseIdText
            //
            lblDatabaseIdText.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblDatabaseIdText.AutoSize = true;
            lblDatabaseIdText.Location = new Point(12, 196);
            lblDatabaseIdText.Name = "lblDatabaseIdText";
            lblDatabaseIdText.Size = new Size(42, 15);
            lblDatabaseIdText.TabIndex = 4;
            lblDatabaseIdText.Text = "Database ID:";
            //
            // lblDatabaseIdValue
            //
            lblDatabaseIdValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblDatabaseIdValue.AutoSize = true;
            lblDatabaseIdValue.Location = new Point(189, 196);
            lblDatabaseIdValue.Name = "lblDatabaseIdValue";
            lblDatabaseIdValue.Size = new Size(46, 15);
            lblDatabaseIdValue.TabIndex = 5;
            lblDatabaseIdValue.Text = "Not Set";
            //
            // lblDevelopmentTokenTxt
            //
            lblDevelopmentTokenTxt.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblDevelopmentTokenTxt.AutoSize = true;
            lblDevelopmentTokenTxt.Location = new Point(12, 225);
            lblDevelopmentTokenTxt.Name = "lblDevelopmentTokenTxt";
            lblDevelopmentTokenTxt.Size = new Size(144, 15);
            lblDevelopmentTokenTxt.TabIndex = 6;
            lblDevelopmentTokenTxt.Text = "Development Token:";
            //
            // lblDevelopmentTokenValue
            //
            lblDevelopmentTokenValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblDevelopmentTokenValue.AutoSize = true;
            lblDevelopmentTokenValue.Location = new Point(189, 225);
            lblDevelopmentTokenValue.Name = "lblDevelopmentTokenValue";
            lblDevelopmentTokenValue.Size = new Size(46, 15);
            lblDevelopmentTokenValue.TabIndex = 7;
            lblDevelopmentTokenValue.Text = "Not Set";
            //
            // lblServerUrlTxt
            //
            lblServerUrlTxt.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblServerUrlTxt.AutoSize = true;
            lblServerUrlTxt.Location = new Point(12, 254);
            lblServerUrlTxt.Name = "lblServerUrlTxt";
            lblServerUrlTxt.Size = new Size(60, 15);
            lblServerUrlTxt.TabIndex = 8;
            lblServerUrlTxt.Text = "Server URL:";
            //
            // lblServerURLValue
            //
            lblServerURLValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblServerURLValue.AutoSize = true;
            lblServerURLValue.Location = new Point(189, 254);
            lblServerURLValue.Name = "lblServerURLValue";
            lblServerURLValue.Size = new Size(46, 15);
            lblServerURLValue.TabIndex = 9;
            lblServerURLValue.Text = "Not Set";
            //
            // btnClose
            //
            btnClose.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            btnClose.Location = new Point(249, 326);
            btnClose.Name = "btnClose";
            btnClose.Size = new Size(75, 23);
            btnClose.TabIndex = 12;
            btnClose.Text = "Close";
            btnClose.UseVisualStyleBackColor = true;
            btnClose.Click += btnClose_Click;
            //
            // AboutForm
            //
            AcceptButton = btnClose;
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(584, 361);
            Controls.Add(btnClose);
            Controls.Add(lblServerURLValue);
            Controls.Add(lblServerUrlTxt);
            Controls.Add(lblDevelopmentTokenValue);
            Controls.Add(lblDevelopmentTokenTxt);
            Controls.Add(lblDatabaseIdValue);
            Controls.Add(lblDatabaseIdText);
            Controls.Add(lblSDKVersion);
            Controls.Add(lblSDKVersionText);
            Controls.Add(llDittoDocs);
            Controls.Add(pictureBox1);
            MaximizeBox = false;
            MaximumSize = new Size(600, 400);
            MdiChildrenMinimizedAnchorBottom = false;
            MinimizeBox = false;
            MinimumSize = new Size(600, 400);
            Name = "AboutForm";
            StartPosition = FormStartPosition.CenterParent;
            Text = "AboutForm";
            ((System.ComponentModel.ISupportInitialize)pictureBox1).EndInit();
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private PictureBox pictureBox1;
        private LinkLabel llDittoDocs;
        private Label lblSDKVersionText;
        private Label lblSDKVersion;
        private Label lblDatabaseIdText;
        private Label lblDatabaseIdValue;
        private Label lblDevelopmentTokenTxt;
        private Label lblDevelopmentTokenValue;
        private Label lblServerUrlTxt;
        private Label lblServerURLValue;
        private Button btnClose;
    }
}