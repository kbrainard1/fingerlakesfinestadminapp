# FLF Admin App

This app allows for easy editing of the site content. Currently supported:

- Adding a new horse listing from a Facebook post
- Marking a horse as placed

# Installing

Not really supported easily right now - clone the repo, load it into an IDE, get a classic token with repo access from Github and store it in "env" in the working directory, compile it, run CreateListingFrontend.

This will improve.

# Using The App

Hopefully self explanatory from the menu buttons. Changes are automatically saved to the cloud. Once you've made all the changes you want for one session, click 'Deploy All Changes To Site'. Wait about 30 seconds (or monitor the fingerlakesfinest repo for when the workflows complete), and then view the changes on the site.

# If a change shows up as broken

Revert the most recent commit to the main branch, and then debug on the staging branch

# Why can't I preview the changes before they go live?

Youtube links require an actual http referrer, which you can't easily get from looking at local files, so 'preview' always looks pretty broken. You can work around that by installing VSCode and viewing the branch with Live Server, but honestly, if you're doing that, you don't need either this tool or this README to guide you.
