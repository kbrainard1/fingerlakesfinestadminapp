# FLF Admin App

This app allows for easy editing of the site content and Facebook page. Currently supported:

- Adding a new horse listing
- Edit an existing horse
- Marking a horse as placed

# Installing

1. Register for an account on github.com
2. Add your Facebook account to the "developers" group (go to developers.facebook.com and log in)
2. Contact Katherine or other site admin with your GitHub username, facebook name, and your operating system (windows, mac, linux) - we'll grant you write access to the site data and send you installation instructions (also available below):

On Windows:

Download and unzip "install.zip"
Double-click on the FLFAdmin app (open like a normal app) - you'll be prompted to log in to GitHub and Facebook to authorize the app
Now you're all set!

On Mac:
Not really supported easily right now - clone the repo, then from the terminal, run `chmod a+rwx gradlew; ./gradlew run`

If someone needs this on a Mac, we can investigate other options, but the requirement that dmg files be signed by a registered Mac developer likely means you'll be messing with the command line anyway

# Using The App

Hopefully self explanatory from the menu buttons. 

Add a horse: Autofills data from Equibase and the FLF Youtube Channel, and allows previewing before publishing to the website and Facebook
Edit a horse: Similar to 'add', allows previewing before publishing to the website and Facebook
Mark as Placed: Automatically publishes to the site and updates the Facebook post, but there is an 'undo' option if you misclick


# Whoops, I made a typo that I spotted in 'Preview'!

Select 'Edit' either on the preview page or from the top level menu to fix or change anything about a horse's page

# FAQ

Q: Why do you have to email me the install file when there's a repo *right here*?

A: We need a marginally more secure than a public repo way to hand out the youtube API key - asking you to log in to your google account is a really heavy-handed way of accessing free-tier quota for public youtube data (especially because the average person isn't intimately familiar with google oauth scopes). The API key has extremely limited scope, so if it leaks it's not that big a deal (Hacker McHackson can send a few more automated searches to Youtube, but not read/edit any additional data), but even so. 

Q: Why does the Equibase auto-fill take such wildly varying amounts of time?

A: Unfortunately, Equibase set up cloudflare without also creating an API with a quota system. There are workarounds, but if, for example, Chrome decides to run an update right after Selenium starts the headless browser, things can go a bit sideways. The good news is that we're in the process of caching the relevant Equibase data, which should not only speed up the whole process, but also make it more reliable. 