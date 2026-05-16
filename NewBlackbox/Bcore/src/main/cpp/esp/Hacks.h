#ifndef DESI_IMPORTANT_HACK_H
#define DESI_IMPORTANT_HACK_H
//#include "DeviceUtils.h"
#include "socket.h"
std::string EXP = "NULL";


Request request;
Response response;
char extra[30];
int botCount, playerCount;
Color clrEnemy, clrFilled, clrEdge, clrBox, clrSkeleton;
Options options { 1, -1, false, 1, false, 201, false };

bool SilentcheatEXP = false;
bool FullDeviceInfo = false;

    
bool isPositionValid(Vec2 position) {
    return (position.x > request.ScreenWidth || position.y > request.ScreenHeight || position.x < 0.0f || position.y < 0.0f);
}

bool isDotUnderCircle(Vec2 pos, float radius) {
    float diffX = abs(pos.x), diffY = abs(pos.y);
    return (diffX * diffX + diffY * diffY) <= radius;
}

int isOutsideSafezone(Vec2 pos, Vec2 screen) {
    Vec2 mSafezoneTopLeft(screen.x * 0.04f, screen.y * 0.04f);
    Vec2 mSafezoneBottomRight(screen.x * 0.96f, screen.y * 0.96f);

    int result = 0;
    if (pos.y < mSafezoneTopLeft.y) {
        result |= 1;
    }
    if (pos.x > mSafezoneBottomRight.x) {
        result |= 2;
    }
    if (pos.y > mSafezoneBottomRight.y) {
        result |= 4;
    }
    if (pos.x < mSafezoneTopLeft.x) {
        result |= 8;
    }
    return result;
}

Vec2 pushToScreenBorder(Vec2 Pos, Vec2 screen, int borders, int offset) {
    int x = (int) Pos.x;
    int y = (int) Pos.y;
    if ((borders & 1) == 1) {
        y = 0 - offset;
    }
    if ((borders & 2) == 2) {
        x = (int) screen.x + offset;
    }
    if ((borders & 4) == 4) {
        y = (int) screen.y + offset;
    }
    if ((borders & 8) == 8) {
        x = 0 - offset;
    }
    return Vec2(x, y);
}

std::string getAMPM() {
    time_t now = time(0);
    struct tm* timeinfo = localtime(&now);
    return (timeinfo->tm_hour < 12) ? "AM IST" : "PM IST";
}

std::string getDayName() {
    time_t now = time(0);
    struct tm* timeinfo = localtime(&now);
    const char* weekday[] = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
    return weekday[timeinfo->tm_wday];
}

void DrawESP(ESP esp, int screenWidth, int screenHeight) {
    botCount = 0;
    playerCount = 0;
    request.ScreenHeight = screenHeight;
    request.ScreenWidth = screenWidth;
    request.options = options;
    request.Mode = InitMode;

    send((void *) &request, sizeof(request));
    receive((void *) &response);
    
    
    
    
    float mScaleX = screenWidth / (float) 2340;
    float mScaleY = screenHeight / (float) 1080;
    Vec2 screen = Vec2(screenWidth, screenHeight);
    Vec2 center = Vec2(screenWidth / 2, screenHeight / 2);

	esp.DrawTextName(Color(255, 0, 0, 255), Vec2(210, 50), (mScaleY * 35), response.Identified);
    
    esp.DrawTextjkpapa(Color(255, 255, 0), Color(255, 51, 51 ), "Mods Loader (3.6)", Vec2(1400, screenHeight / 1.05f), 25);
    
    if(SilentcheatEXP){
       std::string dayName = getDayName();
       std::string ampm = getAMPM();
       std::string expString = dayName + " " + EXP + " " + ampm;
       esp.DrawText(Color(0, 255, 255), expString.c_str(), Vec2(screenWidth / 5.5, screenHeight / 1.05f), 25);
    }  
    SilentcheatEXP = true;
	
    if (response.Success) {
	    if (options.openState == 0) {
            esp.DrawCircle(Color(250, 0, 0), center, request.options.aimingRange, (mScaleY * 1.4f));
        }

        for (int i = 0; i < response.PlayerCount; i++) {
            PlayerData player = response.Players[i];
			
            if (player.isBot) {
                botCount++;
                clrFilled = Color(255, 255, 255, 40);
                clrEnemy = Color(255, 255, 255,255);
                clrEdge = Color(255, 255, 255, 150);
                clrSkeleton = Color(255, 255, 255, 200);
                clrBox = Color(255, 255, 255,200);
            } else {
                playerCount++;
                clrFilled = Color::Transform(player.Distance, 40);
                clrEnemy = Color::Transform(player.Distance, 255);
                clrEdge = Color::Transform(player.Distance, 150);
                clrSkeleton = Color::Transform(player.Distance, 200);
                clrBox = Color::Transform(player.Distance, 200);
            }

            if (player.Precise.X < center.x && player.Precise.Z > center.x && player.Precise.Y < center.y && player.Precise.W > center.y) {
                clrEnemy = Color(0, 255, 0, 255);
                clrSkeleton = Color(0, 255, 0, 200);
                clrBox = Color(0, 255, 0, 200);
                clrFilled = Color(0, 255, 0, 30);
            }

            if (isPositionValid(player.Location)) {
                int borders = isOutsideSafezone(player.Location, screen);
                if (isr360Alert && borders != 0) {
                    Vec2 hintDotRenderPos = pushToScreenBorder(player.Location, screen, borders, (int)(mScaleY * 25));
                    Vec2 hintTextRenderPos = pushToScreenBorder(player.Location, screen, borders, -(int)(mScaleY * 40));
                    esp.DrawFilledCircle(clrEdge, hintDotRenderPos, (mScaleY * 100));
                    sprintf(extra, "%0.0fm", player.Distance);
                    if (player.isBot) {
                    	esp.DrawText(Color(0, 0 ,0, 150), extra, hintTextRenderPos, (mScaleY * 30));
            		}
					else {
						esp.DrawText(Color(255, 255, 255, 150), extra, hintTextRenderPos, (mScaleY * 30));
                	}
		    	}
            }
			else {
                if (isSkelton && player.Bone.isBone) {
					float skelSize = (mScaleY * 3.5f);
					float headsize = (mScaleY * 7.0f);
					float distanceFromCamera = player.Distance;
					float minHeadSize = (mScaleY * 2.0f);
					// Decrease headsize by 0.1 for every unit beyond 10 meters, with a minimum size of 4.0
					headsize = std::max(minHeadSize, headsize - std::min((distanceFromCamera - 10.0f) * 0.1f, 0.1f));
					esp.DrawFilledCircle(clrSkeleton, player.Bone.head, static_cast<int>(headsize));
					std::cout << "Dynamic Head Size: " << headsize << " - Distance: " << distanceFromCamera << "m" << std::endl;
                    esp.DrawLine(clrSkeleton, skelSize,
					    Vec2(player.Bone.head.x, player.Bone.head.y),
                        Vec2(player.Bone.neck.x, player.Bone.neck.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.neck.x, player.Bone.neck.y),
                        Vec2(player.Bone.cheast.x, player.Bone.cheast.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.cheast.x, player.Bone.cheast.y),
                        Vec2(player.Bone.pelvis.x, player.Bone.pelvis.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.neck.x, player.Bone.neck.y),
                        Vec2(player.Bone.lSh.x, player.Bone.lSh.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.neck.x, player.Bone.neck.y),
                        Vec2(player.Bone.rSh.x, player.Bone.rSh.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.lSh.x, player.Bone.lSh.y),
                        Vec2(player.Bone.lElb.x, player.Bone.lElb.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.rSh.x, player.Bone.rSh.y),
                        Vec2(player.Bone.rElb.x, player.Bone.rElb.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.lElb.x, player.Bone.lElb.y),
                        Vec2(player.Bone.lWr.x, player.Bone.lWr.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.rElb.x, player.Bone.rElb.y),
                        Vec2(player.Bone.rWr.x, player.Bone.rWr.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.pelvis.x, player.Bone.pelvis.y),
                        Vec2(player.Bone.lTh.x, player.Bone.lTh.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.pelvis.x, player.Bone.pelvis.y),
                        Vec2(player.Bone.rTh.x, player.Bone.rTh.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.lTh.x, player.Bone.lTh.y),
                        Vec2(player.Bone.lKn.x, player.Bone.lKn.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.rTh.x, player.Bone.rTh.y),
                        Vec2(player.Bone.rKn.x, player.Bone.rKn.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.lKn.x, player.Bone.lKn.y),
                        Vec2(player.Bone.lAn.x, player.Bone.lAn.y));
                    esp.DrawLine(clrSkeleton, skelSize,
                        Vec2(player.Bone.rKn.x, player.Bone.rKn.y),
                        Vec2(player.Bone.rAn.x, player.Bone.rAn.y));
                }
				
				if (isPlayerHead) {
					clrEnemy.a = 150;
                    esp.DrawFilledCircle(clrEnemy, player.Bone.pelvis, (7.5f * mScaleY)); clrEnemy.a = 255;
				}

                if (isPlayerBox == 1) {
                    esp.DrawRect(clrBox, (mScaleY * 3.5f), Vec2(player.Precise.X, player.Precise.Y), Vec2(player.Precise.Z, player.Precise.W));
                    esp.DrawFilledRect(clrFilled, Vec2(player.Precise.X, player.Precise.Y), Vec2(player.Precise.Z, player.Precise.W));
                }
				else if (isPlayerBox == 2) {
                    esp.DrawRect(clrBox, (mScaleY * 3.5f), Vec2(player.Precise.X, player.Precise.Y), Vec2(player.Precise.Z, player.Precise.W));
                }

                if (!player.isKnocked) {
                    if (isPlayerLine == 1) {
                        if (isPlayerHealth) {
                            esp.DrawLine(clrEnemy, (mScaleY * 1.6f), Vec2(center.x, (mScaleY * 118)), Vec2(player.Bone.head.x,player.Precise.Y - (mScaleY * 85)));
                        }
						else {
                            esp.DrawLine(clrEnemy, (mScaleY * 1.6f), Vec2(center.x, (mScaleY * 118)), Vec2(player.Bone.head.x,player.Precise.Y - (mScaleY * 85)));
                        }
                    }
					else if (isPlayerLine == 2) {
                        esp.DrawLine(clrEnemy, (mScaleY * 1.6f), center, Vec2(player.Bone.head.x, player.Precise.Y));
                    }
					else if (isPlayerLine == 3) {
                        esp.DrawLine(clrEnemy, (mScaleY * 1.6f), Vec2(center.x, screenHeight), Vec2(player.Bone.head.x, player.Precise.W));
                    }
                }

                float boxCenterX = (player.Precise.X + player.Precise.Z) / 2;
                
                if (isPlayerHealth && !player.isKnocked) {
                    float healthLength = screenWidth / 30;
                    if (healthLength < mScaleY*30)
                        healthLength = mScaleY*30;
                        Color clrHealth;
                    if (player.Health < 25)
                        clrHealth = Color(255, 0, 0);
                    else if (player.Health < 50)
                        clrHealth = Color(255, 205, 0);
                    else if (player.Health < 75)
                        clrHealth = Color(255, 255, 0);
                    else
                        clrHealth = Color(0, 255, 160);
                    if (player.Health == 0) {
                        esp.DrawText(Color(255, 0, 0), "Knocked", Vec2(boxCenterX, player.Precise.Y - screenHeight / 225), 12); // Assuming 12 as the text size
                    } else {
                        // Background rectangle
                        esp.DrawFilledRect(Color(0, 0, 0, 40), Vec2(boxCenterX - healthLength, player.Precise.Y - screenHeight / 70), Vec2(boxCenterX + healthLength, player.Precise.Y - screenHeight / 42));
                        // Health bar
                        esp.DrawFilledRect(clrHealth, Vec2(boxCenterX - healthLength, player.Precise.Y - screenHeight / 70), Vec2(boxCenterX - healthLength + (2 * healthLength) * player.Health / 100, player.Precise.Y - screenHeight / 42));
                        // Border rectangle
                        esp.DrawRect(Color(0, 0, 0, 120), (1 * mScaleY), Vec2(boxCenterX - healthLength, player.Precise.Y - screenHeight / 70), Vec2(boxCenterX + healthLength, player.Precise.Y - screenHeight / 42));
                    }
                }
                
                /*
				if (isPlayerHealth && !player.isKnocked) {
                    float length = (mScaleY * 112);
          			esp.DrawFilledRect(Color(0, 0, 0, 40),
                        Vec2(boxCenterX - length, player.Precise.Y - (mScaleY * 86)),
                        Vec2(boxCenterX + length, player.Precise.Y - (mScaleY * 42)));
          			esp.DrawRect(Color(0, 0, 0, 120), (3 * mScaleY),
                        Vec2(boxCenterX - length, player.Precise.Y - (mScaleY * 86)),
                        Vec2(boxCenterX + length, player.Precise.Y - (mScaleY * 42)));
          			esp.DrawFilledRect(Color(0, 255, 0, 80),
                        Vec2(boxCenterX - length, player.Precise.Y - (mScaleY * 85)),
                        Vec2(boxCenterX - length + (2 * length) * player.Health / 100, player.Precise.Y - (mScaleY * 43)));
                }*/
               
				/*
                if (isPlayerName && response.Players[i].isBot) {
                    esp.DrawText(Color(255,255,255),"[AI]",
                    Vec2(x, top - screenHeight / 65),textsize-3);                       
                } else if (isPlayerName) {
                    esp.DrawName(Color().White(), response.Players[i].PlayerNameByte, response.Players[i].TeamID,
                    Vec2(x, top - screenHeight / 65), textsize-3); 
            	}
                */
                
                if (isPlayerName || isPlayerTeamID) {
                    esp.DrawName(player.PlayerNameByte, player.TeamID, Vec2(boxCenterX, player.Precise.Y - (mScaleY * 1)));
                }
                
                if (isPlayerDist) {
                    float rightOffset = 1.7;
                    esp.DrawDistance(player.Distance, Vec2(boxCenterX + rightOffset, player.Precise.Y - (mScaleY * 60)), mScaleY * 55);
                }
                
                
                if (isEnemyWeapon) {
                    float rightOffset = 1.7;
                    esp.DrawWeapon(Color::GetColor(player.TeamID), player.Weapon.id, player.Weapon.ammo, player.Weapon.ammo, Vec2(boxCenterX + rightOffset, player.Precise.W + (mScaleY * 27)), mScaleY * 50);
                }

                
                
            }
        }
        
        
        if (isGrenadeWarning) {
            for (int i = 0; i < response.GrenadeCount; i++) {
                if (!isPositionValid(response.Grenade[i].Location)) {
                    if (response.Grenade[i].type == 1) {
                        sprintf(extra, "Grenade(%0.0f)", response.Grenade[i].Distance);
                        esp.DrawCircle(FRAGColor(),
                            Vec2(response.Grenade[i].Location.x, response.Grenade[i].Location.y), mScaleY * 18, mScaleY * 4);
                        esp.DrawText1(FRAGColor(), extra,
                            Vec2(response.Grenade[i].Location.x, response.Grenade[i].Location.y - 30), mScaleY * 25);
                    }
                    else if (response.Grenade[i].type == 2) {
                        sprintf(extra, "Molotov(%0.0f)", response.Grenade[i].Distance);
                        esp.DrawCircle(FRAGColor(),
                            Vec2(response.Grenade[i].Location.x, response.Grenade[i].Location.y), mScaleY * 18, mScaleY * 4);
                        esp.DrawText1(FRAGColor(), extra,
                            Vec2(response.Grenade[i].Location.x, response.Grenade[i].Location.y - 30), mScaleY * 25);
                    }
                }
            }
        }

        for (int i = 0; i < response.VehicleCount; i++) {
            if (!isPositionValid(response.Vehicles[i].Location)) {
                esp.DrawVehicles(response.Vehicles[i].VehicleName, response.Vehicles[i].Distance, response.Vehicles[i].Health, response.Vehicles[i].Fuel,
                    Vec2(response.Vehicles[i].Location.x, response.Vehicles[i].Location.y));
            }
        }
		
        for (int i = 0; i < response.ItemsCount; i++) {
            if (!isPositionValid(response.Items[i].Location)) {
                esp.DrawItems(response.Items[i].ItemName, response.Items[i].Distance,
                    Vec2(response.Items[i].Location.x, response.Items[i].Location.y));
            }
        }
    }

    if (botCount + playerCount > 0) {
        esp.DrawEnemyCount(Color(255, 175, 20),
        Vec2(screenWidth / 2 - screenHeight / 9, screenHeight / 14.2f),
        Vec2(screenWidth / 2 + screenHeight / 9, screenHeight / 9.3f));
        sprintf(extra, "%d", playerCount + botCount);
        esp.DrawText(Color(0, 0, 0), extra, Vec2(screenWidth / 2, screenHeight / 10), screenHeight / 35);
    } else {
        esp.DrawEnemyCount(Color(91, 255, 79),
        Vec2(screenWidth / 2 - screenHeight / 9, screenHeight / 14.2f),
        Vec2(screenWidth / 2 + screenHeight / 9, screenHeight / 9.3f));
        esp.DrawText(Color(0, 0, 0), OBFUSCATE("CLEAR"), Vec2(screenWidth / 2, screenHeight / 10), screenHeight / 35);
    }
    
    
    
}
#endif // DESI_IMPORTANT_HACK_H
