-- mod-version:3

--[[
 c3ds-projects - Assorted compatibility fixes & useful tidbits
 Written starting in 2022 by contributors (see CREDITS.txt)
 To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
]]

-- lite-xl 2.1 syntax highlighting module for RAL

local syntax = require("core.syntax")

local ral_loners_class = "%;%[%]%{%}%(%)%,%."
local ral_operators_breaking_class = "%<%>%=%?%!%/%*%-%+%:%&%|%^%%%~"
local ral_operators_class = ral_operators_breaking_class .. ""
local ral_breakers_class = "%c%s%\"%'" .. ral_loners_class .. ral_operators_breaking_class

local function gen_ral_patterns(se_syntax)
	return {
		-- pretty normal
		{pattern = { "%/%*", "%*%/"}, type = "comment"},
		{pattern = "%/%/.*", type = "comment"},
		-- separated into two patterns to replicate "needs at least one body"
		{pattern = "[0123456789][0123456789%.e]*", type = "number"},
		{pattern = "[%+%-][0123456789%.e]+", type = "number"},
		-- strings
		{pattern = { '"', '"', "\\" }, type = "string"},
		{pattern = { "'", "'", "\\" }, type = "string", syntax = se_syntax},
		-- main breakdown
		{pattern = ral_loners_class, type = "operator"},
		{pattern = "[" .. ral_operators_class .. "]+", type = "operator"},
		-- symbols breakdown w/ special cases
		{pattern = "[^" .. ral_breakers_class .. "]+()%s*%f[%(]", type = {"function", "normal"}},
		-- convention is that user-defined types and constants start with a capital letter
		{pattern = "[A-Z][^" .. ral_breakers_class .. "]*", type = "keyword2"},
		-- symbol base
		{pattern = "[^" .. ral_breakers_class .. "]+", type = "symbol"},
	}
end

local ral_symbols = {
	-- meta
	["include"] = "keyword",
	["addSearchPath"] = "keyword",
	-- type system declarations
	["class"] = "keyword",
	["interface"] = "keyword",
	["extends"] = "keyword",
	["typedef"] = "keyword",
	["message"] = "keyword",
	["field"] = "keyword",
	["assertConst"] = "keyword",
	-- evil decls
	["overrideOwnr"] = "keyword",
	["messageHook"] = "keyword",
	-- code declarations
	["macro"] = "keyword",
	["script"] = "keyword",
	["install"] = "keyword",
	["remove"] = "keyword",
	-- in-function
	["let"] = "keyword",
	["if"] = "keyword",
	["else"] = "keyword",
	["while"] = "keyword",
	["break"] = "keyword",
	["alias"] = "keyword",
	["return"] = "keyword",
	["instanceof"] = "keyword",
	["with"] = "keyword",
	["after"] = "keyword",
	["for"] = "keyword",
	["foreach"] = "keyword",
	["in"] = "keyword",
	["call"] = "keyword",
	["class"] = "keyword",
	-- compiler types
	["str"] = "keyword2",
	["int"] = "keyword2",
	["bool"] = "keyword2",
	["float"] = "keyword2",
	["num"] = "keyword2",
	["null"] = "keyword2",
	-- compiler helpers
	["true"] = "keyword2",
	["false"] = "keyword2",
}

syntax.add({
	name = "RAL",
	files = "%.ral$",
	headers = "^#!.*[ /]lua",
	comment = "//",
	block_comment = { "/*", "*/" },
	patterns = gen_ral_patterns(".ral-string-embed"),
	symbols = ral_symbols,
})

syntax.add({
	name = "RAL String Embed",
	files = "%.ral%-string%-embed$",
	headers = "^#!.*[ /]lua",
	comment = "//",
	block_comment = { "/*", "*/" },
	patterns = {
		-- the %f[%}] is a blatant workaround because otherwise the string end is missed if the } is at the edge of a string
		{pattern = {"%{", "%f[%}]"}, type = "string", syntax = ".ral-string-embed-interior"},
		{pattern = ".", type = "string"}
	},
	symbols = {
		-- ???
	},
})

syntax.add({
	name = "RAL String Embed Interior",
	files = "%.ral%-string%-embed%-interior$",
	headers = "^#!.*[ /]lua",
	comment = "//",
	block_comment = { "/*", "*/" },
	-- going past here WILL crash lite-xl
	patterns = gen_ral_patterns(nil),
	symbols = ral_symbols,
})

