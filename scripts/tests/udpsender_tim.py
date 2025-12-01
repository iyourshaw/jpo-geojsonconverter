import socket
import time
import os

# Currently set to oim-dev environment's ODE
UDP_IP = os.getenv('DOCKER_HOST_IP')
UDP_PORT = 47900
MESSAGES = [
  "001f436015d1f618d4a500000d3a14a508080000fd2c5ee1f4008006e53b4b8934e5efe1f479d00b740f000045bc6d7b2629dd33fbf96038d52f8b88350348000021b040a058"
]

print("UDP target IP:", UDP_IP)
print("UDP target port:", UDP_PORT)

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP

print("sending TIM every 5 second")

while True:
  print ("sending TIM")
  for MESSAGE in MESSAGES:
    sock.sendto(bytes.fromhex(MESSAGE), (UDP_IP, UDP_PORT))
  time.sleep(5)
