
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
            lblAppIDText = new Label();
            lblAppIdValue = new Label();
            lblOnlinePlaygroundTokenTxt = new Label();
            lblOnlinePlaygroundTokenValue = new Label();
            lblAuthUrlTxt = new Label();
            lblAuthURLValue = new Label();
            lblWebsocketUrlTxt = new Label();
            lblWebsocketURLValue = new Label();
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
            // lblAppIDText
            // 
            lblAppIDText.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblAppIDText.AutoSize = true;
            lblAppIDText.Location = new Point(12, 196);
            lblAppIDText.Name = "lblAppIDText";
            lblAppIDText.Size = new Size(42, 15);
            lblAppIDText.TabIndex = 4;
            lblAppIDText.Text = "AppId:";
            // 
            // lblAppIdValue
            // 
            lblAppIdValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblAppIdValue.AutoSize = true;
            lblAppIdValue.Location = new Point(189, 196);
            lblAppIdValue.Name = "lblAppIdValue";
            lblAppIdValue.Size = new Size(46, 15);
            lblAppIdValue.TabIndex = 5;
            lblAppIdValue.Text = "Not Set";
            // 
            // lblOnlinePlaygroundTokenTxt
            // 
            lblOnlinePlaygroundTokenTxt.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblOnlinePlaygroundTokenTxt.AutoSize = true;
            lblOnlinePlaygroundTokenTxt.Location = new Point(12, 225);
            lblOnlinePlaygroundTokenTxt.Name = "lblOnlinePlaygroundTokenTxt";
            lblOnlinePlaygroundTokenTxt.Size = new Size(144, 15);
            lblOnlinePlaygroundTokenTxt.TabIndex = 6;
            lblOnlinePlaygroundTokenTxt.Text = "Online Playground Token:";
            // 
            // lblOnlinePlaygroundTokenValue
            // 
            lblOnlinePlaygroundTokenValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblOnlinePlaygroundTokenValue.AutoSize = true;
            lblOnlinePlaygroundTokenValue.Location = new Point(189, 225);
            lblOnlinePlaygroundTokenValue.Name = "lblOnlinePlaygroundTokenValue";
            lblOnlinePlaygroundTokenValue.Size = new Size(46, 15);
            lblOnlinePlaygroundTokenValue.TabIndex = 7;
            lblOnlinePlaygroundTokenValue.Text = "Not Set";
            // 
            // lblAuthUrlTxt
            // 
            lblAuthUrlTxt.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblAuthUrlTxt.AutoSize = true;
            lblAuthUrlTxt.Location = new Point(12, 254);
            lblAuthUrlTxt.Name = "lblAuthUrlTxt";
            lblAuthUrlTxt.Size = new Size(60, 15);
            lblAuthUrlTxt.TabIndex = 8;
            lblAuthUrlTxt.Text = "Auth URL:";
            // 
            // lblAuthURLValue
            // 
            lblAuthURLValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblAuthURLValue.AutoSize = true;
            lblAuthURLValue.Location = new Point(189, 254);
            lblAuthURLValue.Name = "lblAuthURLValue";
            lblAuthURLValue.Size = new Size(46, 15);
            lblAuthURLValue.TabIndex = 9;
            lblAuthURLValue.Text = "Not Set";
            // 
            // lblWebsocketUrlTxt
            // 
            lblWebsocketUrlTxt.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblWebsocketUrlTxt.AutoSize = true;
            lblWebsocketUrlTxt.Location = new Point(12, 283);
            lblWebsocketUrlTxt.Name = "lblWebsocketUrlTxt";
            lblWebsocketUrlTxt.Size = new Size(92, 15);
            lblWebsocketUrlTxt.TabIndex = 10;
            lblWebsocketUrlTxt.Text = "Websocket URL:";
            // 
            // lblWebsocketURLValue
            // 
            lblWebsocketURLValue.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            lblWebsocketURLValue.AutoSize = true;
            lblWebsocketURLValue.Location = new Point(189, 283);
            lblWebsocketURLValue.Name = "lblWebsocketURLValue";
            lblWebsocketURLValue.Size = new Size(46, 15);
            lblWebsocketURLValue.TabIndex = 11;
            lblWebsocketURLValue.Text = "Not Set";
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
            Controls.Add(lblWebsocketURLValue);
            Controls.Add(lblWebsocketUrlTxt);
            Controls.Add(lblAuthURLValue);
            Controls.Add(lblAuthUrlTxt);
            Controls.Add(lblOnlinePlaygroundTokenValue);
            Controls.Add(lblOnlinePlaygroundTokenTxt);
            Controls.Add(lblAppIdValue);
            Controls.Add(lblAppIDText);
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
        private Label lblAppIDText;
        private Label lblAppIdValue;
        private Label lblOnlinePlaygroundTokenTxt;
        private Label lblOnlinePlaygroundTokenValue;
        private Label lblAuthUrlTxt;
        private Label lblAuthURLValue;
        private Label lblWebsocketUrlTxt;
        private Label lblWebsocketURLValue;
        private Button btnClose;
    }
}