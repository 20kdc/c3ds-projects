#!/usr/bin/env python3
# Decomplicates things, I promise.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import math

# See visscript section of HELP.md

class TestCtx():
	def exists(self, prop):
		return prop != "noexist"
	def get(self, prop):
		return prop

VISSCRIPT_TKN = ["(", ")", "&&", "|", "&", "!", "="]

VISSCRIPT_OPS = ["&&", "|", "&", "!", "="]

def visscript_assert_oztkn(tkns):
	if len(tkns) == 0:
		return ""
	if len(tkns) != 1:
		raise Exception("VisScript: Unaccounted-for operator: " + str(tkns))
	return tkns[0]

def visscript_compile_op(l, op, r):
	if op == "|":
		ls = visscript_compile(l)
		rs = visscript_compile(r)
		return lambda ctx: ls(ctx) or rs(ctx)
	elif op == "&" or op == "&&":
		ls = visscript_compile(l)
		rs = visscript_compile(r)
		return lambda ctx: ls(ctx) and rs(ctx)
	elif op == "=":
		prop = visscript_assert_oztkn(l).lower()
		val = visscript_assert_oztkn(r).lower()
		return lambda ctx: ctx.exists(prop) and (ctx.get(prop).lower() == val)
	elif op == "!":
		if visscript_assert_oztkn(l) != "":
			raise Exception("VisScript: Cannot use ! after something")
		rs = visscript_compile(r)
		return lambda ctx: not rs(ctx)
	else:
		# shouldn't even be possible
		raise Exception("VisScript: Unknown op: " + op)

def visscript_truthy(val):
	val = str(val)
	if val == "":
		return False
	elif val == "0" or val == "0.0":
		return False
	return True

def visscript_tokenize(script):
	script = script.strip()
	if script == "":
		return []
	# Find first matching token...
	for tkn in VISSCRIPT_TKN:
		res = script.split(tkn)
		# If it's there and thus splits the array...
		if len(res) != 1:
			out = []
			first = True
			for elm in res:
				if not first:
					out.append(tkn)
				out += visscript_tokenize(elm)
				first = False
			return out
	return [script.strip()]

def visscript_compile(tkns):
	# 'Set' component (lighting cams etc.) : always valid
	if len(tkns) == 0:
		return lambda ctx: True
	# flag: prop is present and non-zero
	if len(tkns) == 1:
		prop = tkns[0].lower()
		return lambda ctx: ctx.exists(prop) and visscript_truthy(ctx.get(prop))
	# otherwise, try to find op
	op_idx = None
	op_kind = len(VISSCRIPT_OPS)
	depth = 0
	for idx in range(len(tkns)):
		tkn = tkns[idx]
		if tkn == "(":
			depth += 1
		elif tkn == ")":
			depth -= 1
		elif depth == 0:
			if tkn in VISSCRIPT_OPS:
				new_op_kind = VISSCRIPT_OPS.index(tkn)
				if new_op_kind < op_kind:
					op_idx = idx
					op_kind = new_op_kind
	if depth != 0:
		raise Exception("VisScript: Unmatched parens or unknown operator: " + str(tkns))
	if not op_idx is None:
		return visscript_compile_op(tkns[:op_idx], VISSCRIPT_OPS[op_kind], tkns[op_idx + 1:])
	# unable to divide by op, but parens are balanced. try for paren depth
	if len(tkns) >= 2:
		if tkns[0] == "(" and tkns[len(tkns) - 1] == ")":
			return visscript_compile(tkns[1:len(tkns) - 1])
	# outta ideas
	raise Exception("VisScript: unparsable: " + str(tkns))

