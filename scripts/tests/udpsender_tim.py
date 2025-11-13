import socket
import time
import os

# Currently set to oim-dev environment's ODE
UDP_IP = os.getenv('DOCKER_HOST_IP')
UDP_PORT = 47900
MESSAGE = "001F356005B55F90C1934E9FED935B7308080000FD2B6ABE9C668007E132C557808CD7CA2401924000108006186110F239AC7EA000200401"

print("UDP target IP:", UDP_IP)
print("UDP target port:", UDP_PORT)

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP

while True:
  time.sleep(5)
  print("sending BSM every 5 second")
  sock.sendto(bytes.fromhex(MESSAGE), (UDP_IP, UDP_PORT))
