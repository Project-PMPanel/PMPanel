### 2021年9月3日

- 设置用户界面弹窗公告的默认值(默认不弹窗)
- 修复了1.0.0版本的注册bug(数据库passwd字段长度问题)
- 修复了管理员界面无法显示与修改用户仪表盘弹窗通知的bug
- 修复了用户界面流量详情天数的边界bug
- 修复了通知弹窗在移动端显示bug
- 新增管理员界面当年的收入详情表格
- 更新邮件模板

sql:
- 删除config表中renewMail的值,替换为mailTemplate, 变动如下
```
INSERT INTO `config`(`id`, `name`, `value`) VALUES (402, 'renewMail', '<div style=\"text-align: center;background-color: #eaeaea;width: 80%;height: 80%;margin: auto;position: absolute;top: 0;left: 0;right: 0;bottom: 0;\">\n	<span style=\"display: block;text-align: center;color: #fff;font-size: 26px;height: 50px;line-height: 50px;background: #71c4ff;\">{siteName}</span>\n	<span style=\"display: block;margin-top: 20px;font-size: 20px\">\n		您收到此邮件是因为您在{siteName}的会员即将过期<br><br>\n		为保证服务正常使用,请尽快续费<br><br>\n		官网地址: <a href=\"{siteUrl}\">{siteUrl}</a>\n	</span>\n</div>');
# 上方的删除了,下方的新增了
INSERT INTO `config`(`id`, `name`, `value`) VALUES (402, 'mailTemplate', '<div id=\"mail\">\r\n		<div id=\"main\">\r\n			<div id=\"header\">\r\n				{siteName}\r\n			</div>\r\n			<div id=\"title\">\r\n				{title}\r\n			</div>\r\n			<hr>\r\n			<div id=\"content\">\r\n				{content}\r\n			</div>\r\n			<hr>\r\n			<div id=\"footer\">\r\n				<a href=\"{siteUrl}\" style=\"margin-left: 1%;color: #929292;text-decoration:none\">前往{siteName}</a>\r\n			</div>\r\n		</div>\r\n	</div>\r\n\r\n	<style type=\"text/css\">\r\n		#mail {\r\n			background: #48484845;\r\n			padding: 1%;\r\n		}\r\n		#main {\r\n			margin: 0 30% 0 30%;\r\n			line-height: 20px;\r\n			background: white;\r\n		}\r\n		#header {\r\n			width: 100%;\r\n			height: 10%;\r\n			background: #4880c5;\r\n			font-size: 40px;\r\n			line-height: 80px;\r\n			text-align: center;\r\n			color: white;\r\n		}\r\n		#title {\r\n			height: 40px;\r\n			line-height: 40px;\r\n			font-size: 24px;\r\n			margin: 2%;\r\n			padding-left: 2%;\r\n		}\r\n		#content {\r\n			margin: 2% 0 0 2%;\r\n			font-size: 20px;\r\n			line-height: 1.5;\r\n			padding: 1% 0 5% 2%;\r\n		}\r\n		#footer {\r\n			height: 40px;\r\n			line-height: 40px;\r\n			font-size: 18px;\r\n			padding-left: 2%;\r\n		}\r\n	</style>');
```


---

### 2021年9月3日 1.0.0 RELEASED

```
建议全新安装,从这个版本开始记录更新日志

前端更新,只需要替换前端文件即可,不做详细描述

后端更新,可能涉及到数据库字段更改等操作,在本文件中将详细说明
```