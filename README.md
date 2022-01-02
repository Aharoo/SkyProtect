# SkyProtect
Application for distributed big file upload to 4 different clouds: Amazon S3, Google Storage, Azure Blob Storage and NextCloud.

First of all, you need to you need to fill up the accounts.properties file (you can find it inside the config folder) with Amazon key pair and create environment variables for Google and Azure. 
To fill up this file you need first create accounts in the cloud providers. 
To do that follow the links below:
To start working with the software product, you need to create accounts on AWS, Microsoft Azure, GCP and install NextCloud. After that, you need to obtain API access keys on each platform.
---
- Amazon Web Services.To find the Amazon S3 keys, go to the AWS Management Console, click on the S3 service, then select your account name in the upper right corner and go to the security credentials. Then generate your access keys and secret keys.
![Amazon](https://i.ibb.co/XyGbk23/amazon.jpg)
- Microsoft Azure. To find Google Storage keys, go to the Google API Console, then to the Google Cloud Storage splitter. Choose API & Services, in the section we find the Credentials section and there we find access keys.
![Microsoft](https://i.ibb.co/C1BXmw8/azure.png)
- Google Cloud Platform. To find Windows Azure keys, go to the Windows Azure portal. First you need to create a new repository project. After selecting this new project, you can find key management at the bottom of the page. In this case, the access key is the name of the repository project, and the secret key is the primary key in key management.
![Google](https://i.ibb.co/0r8CR9Y/google.png)
- NextCloud. To connect to NextCloud you need to get the URL to the WebDAV protocol, which is in the settings in the lower left corner.
![NextCloud](https://i.ibb.co/hWQwRbn/nextcloud.png)



Available commands:
pick 'name'     - change the container
write 'data'    - write a new version in the selected container
en_write 'data' - write a new version with additional encryption
download 'path' - download the last version of the selected container
get_all_files   - return list of existing files in containers
get_all_units   - return list of existing units
delete_all      - delete all the files in the selected container
hash_download 'num' - download old versions, you need to enter filename
help            - shows list of available commands"
exit            - stop the program

ATTENTION! Multi-threaded file upload needs some work, please be aware of that before using this application. Thanks for your attention and good luck!
