#!/usr/bin/env python3

# c3ds-projects - Assorted compatibility fixes & useful tidbits
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

#-------------------------------------------------#
# Natsue0 - Prototyping server for Project Natsue #
#-------------------------------------------------#

# This server's goal is to record everything and do as little as possible.

import socket
import struct
import math
import time
import traceback
import sys

# -- libraryey stuff --

log = open("log/" + str(int(math.floor(time.time()))) + ".snoop", "wb")
log.write(b"snoop\x00\x00\x00\x00\x00\x00\x02\x00\x00\x00\x09")
log_buffer = b""

def cut_log():
	global log_buffer
	time_sd = time.time()
	time_s = int(math.floor(time_sd))
	time_sw = time_s & 0xFFFFFFFF # quick, how fast can you say YEAR 2038 PROBLEM!
	time_us = int(math.floor((time_sd - time_s) * 1000000))
	log.write(struct.pack(">IIIIII", len(log_buffer), len(log_buffer), len(log_buffer) + 24, 0, time_sw, time_us))
	log.write(log_buffer)
	log.flush()
	log_buffer = b""

def ral(s: socket.socket, l: int) -> bytes:
	global log_buffer
	data = b""
	while len(data) < l:
		chunk = s.recv(l - len(data))
		log_buffer += chunk
		if chunk == b"":
			raise EOFError("at " + str(len(data)) + " during recvall(" + str(s) + ", " + str(l) + ")")
		data += chunk
	return data

def g32(b: bytes, ofs):
	return struct.unpack("<I", b[ofs:ofs + 4])[0]

# -- BUINs --

server_buin = b"\x01\x00\x00\x00" + b"\x02\x00\x00\x00"
user_buin = b"\x03\x00\x00\x00" + b"\x04\x00\x00\x00"
other_buin = b"\x05\x00\x00\x00" + b"\x06\x00\x00\x00" # for NET: RUSO so that natsue0 has someone to send Norns to

# -- it begins --

send_after_login = b""

if len(sys.argv) == 2:
	ftmp = open(sys.argv[1], "rb")
	send_after_login = ftmp.read()
	ftmp.close()

server_socket = socket.create_server(("127.0.0.1", 49152))

def handle_conn(s: socket.socket):
	global send_after_login
	# expect a handshake packet
	base = ral(s, 0x34)
	if base[0] != 0x25:
		raise Exception("Handshake packet must start with 0x25")
	username_len = g32(base, 0x2C)
	password_len = g32(base, 0x30)
	ral(s, username_len + password_len)
	# we got it, cut log
	cut_log()
	# send back what it wants to hear
	base = b"\x0A\x00\x00\x00" + server_buin + user_buin + b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
	# +32
	base += b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
	# +44
	base += b"\x0C\x00\x00\x00"
	# +48
	base += b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
	s.sendall(base)
	# right, we're officially logged in!
	s.sendall(send_after_login)
	# now we get the lovely task of watching for the various CTOS packets and pretending everything is fine, just FINE
	while True:
		base = ral(s, 0x20)
		# common stuff that has to be looked at or checked or such
		# remember this is a workbench, optimization is not key
		further_data = g32(base, 0x18)
		ticket_number = base[0x14:0x18]
		lookup_uid = g32(base, 12)
		lookup_hid = g32(base, 16)
		lookup_str = str(lookup_uid) + "+" + str(lookup_hid)
		lookup_name = lookup_str.encode("latin1")
		lookup_user = struct.pack("<IIIIII", len(lookup_name) + 32, lookup_uid, lookup_hid, 4, 4, len(lookup_name)) + b"nonenone" + lookup_name
		#                  T               A               B               C               D               T                     F               E
		blank_response = b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" + ticket_number + b"\x00\x00\x00\x00\x00\x00\x00\x00"
		if base[0] == 0x09:
			# C_TID_MESSAGE_CTOS
			print("C_TID_MESSAGE_CTOS")
			ral(s, 8 + further_data)
		elif base[0] == 0x0F:
			# C_TID_GET_CLIENT_INFO
			# give some details for debugging
			print("C_TID_GET_CLIENT_INFO: " + lookup_str)
			#           T               A               B               C               D                   T               F                                       E
			s.sendall(b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" + ticket_number + struct.pack("<I", len(lookup_user)) + b"\x00\x00\x00\x00" + lookup_user)
		elif base[0] == 0x10:
			# C_TID_WWR add
			print("C_TID_WWR add: " + lookup_str)
			# respond by pretending they always were there
			#           T               A               B               C               D               T                   F                                       E
			s.sendall(b"\x0D\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" + struct.pack("<I", len(lookup_user)) + b"\x00\x00\x00\x00" + lookup_user)
		elif base[0] == 0x13:
			# C_TID_GET_CONNECTION_DETAIL
			# everybody we ask for is online, honest!
			print("C_TID_GET_CONNECTION_DETAIL: " + lookup_str)
			#           T               A               B               C               D                   T                 F               E
			s.sendall(b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" + ticket_number + b"\x00\x00\x00\x00\x01\x00\x00\x00")
		elif base[0] == 0x14:
			# C_TID_CLIENT_COMMAND
			print("C_TID_CLIENT_COMMAND")
			ral(s, 4)
		elif base[0] == 0x18:
			# C_TID_GET_STATUS
			print("C_TID_GET_STATUS")
			#          A               B               C               D
			status = b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
			s.sendall(blank_response + status)
		elif base[0] == 0x1E:
			# C_TID_VIRTUAL_CONNECT
			print("C_TID_VIRTUAL_CONNECT")
			ral(s, 12)
		elif base[0] == 0x1F:
			# C_TID_VIRTUAL_CIRCUIT
			print("C_TID_VIRTUAL_CIRCUIT")
			ral(s, 12 + further_data)
		elif base[0] == 0x21:
			if base[1] == 0x02:
				# C_TID_DS_FETCH_RANDOM_USER
				print("C_TID_DS_FETCH_RANDOM_USER")
				#           T               A               B               CD               T                 F               E
				s.sendall(b"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" + other_buin + ticket_number + b"\x00\x00\x00\x00\x01\x00\x00\x00")
			elif base[1] == 0x03:
				# C_TID_DS_FEED_HISTORY
				print("C_TID_DS_FEED_HISTORY")
				ral(s, further_data)
				s.sendall(blank_response)
		else:
			print("packet with no special handling, i.e. removal from WWR, etc.")
		cut_log()

while True:
	skt = server_socket.accept()
	try:
		handle_conn(skt[0])
	except Exception as error:
		traceback.print_exception(type(error), error, error.__traceback__)
		try:
			cut_log()
		except:
			# NOTHING IS WRONG
			pass

