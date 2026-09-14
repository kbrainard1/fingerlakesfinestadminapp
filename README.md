# FLF Admin App

This app allows for easy editing of the site content. Currently supported:

- Adding a new horse listing from a Facebook post
- Adding a new horse listing by filling out the horse info
- Marking a horse as placed

# Installing

1. Register for an account on github.com
2. Contact Kathering or other site admin with your GitHub username and your operating system (windows, mac, linux) - we'll grant you write access to the site data and send you installation instructions (also available below):

On Windows:

Download and unzip "install.zip"
Double-click on the FLFAdmin app (open like a normal app) - you'll be prompted to log in to GitHub and authorize the app
Now you're all set!

On Mac:
Not really supported easily right now - clone the repo, then from the terminal, run `chmod a+rwx gradlew; ./gradlew run`

If someone needs this on a Mac, we can investigate other options, but the requirement that dmg files be signed by a registered Mac developer likely means you'll be messing with the command line anyway

# Using The App

Hopefully self explanatory from the menu buttons. Each task saves data to the cloud, so you can, for example, mark a horse as placed, then exit, and when you come back, they will still be marked as placed.

 Once you've made all the changes you want for one session, click 'Deploy Changes To Site' - you can preview the changes before they go live 

# Whoops, I made a typo that I spotted in 'Preview'!

Select 'Edit an Existing Horse's Info' to fix or change anything about a horse's page

# FAQ

Q: Why do you have to email me the install file when there's a repo *right here*?

A: We need a marginally more secure than a public repo way to hand out the youtube API key - asking you to log in to your google account is a really heavy-handed way of accessing free-tier quota for public youtube data (especially because the average person isn't intimately familiar with google oauth scopes). The API key has extremely limited scope, so if it leaks it's not that big a deal (Hacker McHackson can send a few more automated searches to Youtube, but not read/edit any additional data), but even so. If we really wanted to go crazy, we could use an encrypted file transfer system rather than smtp, but this feels like the right balance of security and usability (folks have to have an email exchange for repo permissions anyway).