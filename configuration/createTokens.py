import os
import binascii
import json

def generateToken(numberTokens):
    for i in range(numberTokens):
        token = binascii.hexlify(os.urandom(5)).decode("utf-8")
        print(token)

if __name__ == "__main__":
    generateToken(3)
