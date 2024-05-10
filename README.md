# What is this

This is the desktop UI that communicates with Linic Cloud backend. This app is running with the OSGi Equinox runtime and it's modularized into different plugins that provide different functionalities, which can be added, or removed from the product based on your needs. Each plugin should have a README.md that describes what it does and how to customize it.

# How to use

Run the product in `ro.linic.ui.product`

# Development

You can run the following maven goals from the root folder:

- `package` to package all plugins, except products
- `package -P product` to package products
- `package -P deploy "-Drepository.path=C:/work/repository"` to deploy the p2 update site to repository.path location(note: this appends to the repository, you will have to increase the plugin version to actually update the repo, if you already ran this command)