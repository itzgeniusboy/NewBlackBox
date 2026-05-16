#ifndef PAKC_IMPORT_H
#define PAKC_IMPORT_H

#include <jni.h>
#include <string>
#include <cstdlib>
#include <unistd.h>
#include <sys/mman.h>
#include <android/log.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <cerrno>
#include <sys/un.h>
#include <cstring>
#include <string>
#include <cmath>
#include "struct.h"

int isPlayerBox = 0, isPlayerLine = 0;
bool isPlayerTeamID = true,
	isPlayerDist = true,
	isPlayerHealth = true,
	isPlayerName = true,
	isPlayerHead = true,
	isr360Alert = true,
	isSkelton = true,
	isGrenadeWarning = true,
    isBoxItemData = true,
	isEnemyWeapon = true;
    
	

#endif // PAKC_IMPORT_H
