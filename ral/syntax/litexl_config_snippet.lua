local lsp = require "plugins.lsp"

lsp.add_server {
	name = "ral",
	language = "RAL",
	file_patterns = { "%.ral$" },
	-- change to lspLog to create a debug "lsp.log" file in your standard library include directory
	command = { "ral", "lsp" },
	requests_in_chunks = true
}

