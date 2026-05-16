#ifndef PAKC_STRUCT_H
#define PAKC_STRUCT_H

#include <string>
#include <iostream>
#include <cstdlib>
#include <ctime>

#define maxplayerCount 100
#define maxvehicleCount 50
#define maxitemsCount 400
#define maxgrenadeCount 10
#define maxboxitemsCount 150

class Color {
    public:
    float r;
    float g;
    float b;
    float a;

    Color() {
        this -> r = 0;
        this -> g = 0;
        this -> b = 0;
        this -> a = 0;
    }

    Color(float r, float g, float b, float a) {
        this -> r = r;
        this -> g = g;
        this -> b = b;
        this -> a = a;
    }

    Color(float r, float g, float b) {
        this -> r = r;
        this -> g = g;
        this -> b = b;
        this -> a = 255;
    }

    static Color White() {
        return Color(255, 255, 255);
    }

    static Color Green() {
        return Color(0, 255, 0);
    }
	
	static Color Transform(int d, int a) {
		return Color(255, d > 255 ? 255 : d, 0, a);
	}

    static Color GetColor(int index, int a = 255) {
        srand(index);
        int r = rand() % 256;
        int g = rand() % 256;
        int b = rand() % 256;
        return Color(r, g, b, a);
    }
};

int randomer;
Color FRAGColor() {
    randomer += 4;
    if (randomer == 0) {
        return Color(255, 165, 0);
    } else if (randomer == 1) {
        return Color(255, 165, 0);
    }else if (randomer == 2) {
        return Color(255, 165, 0);
    }else if (randomer == 3) {
        return Color(255, 165, 0);
    }else if (randomer == 4) {
        return Color(255, 165, 0);
    }else if (randomer == 5) {
        return Color(255, 165, 0);
    }else if (randomer == 6) {
        return Color(255, 165, 0);
    }else if (randomer == 7) {
        return Color(255, 165, 0);
    }else if (randomer == 8) {
        return Color(255, 165, 0);
    }else if (randomer == 9) {
        return Color(255, 165, 0);
    }else if (randomer == 10) {
        return Color(255, 165, 0);
    }else if (randomer == 11) {
        return Color(255, 165, 0);
    }else if (randomer == 12) {
        return Color(255, 165, 0);
    }else if (randomer == 13) {
        return Color(255, 165, 0);
    }else if (randomer == 14) {
        return Color(255, 165, 0);
    }else if (randomer == 15) {
        return Color(255, 165, 0);
    }else if (randomer == 16) {
        return Color(255, 165, 0);
    }else if (randomer == 17) {
        return Color(255, 165, 0);
    } else if (randomer == 18) {
        return Color(255, 165, 0);
    } else if (randomer == 19) {
        return Color(255, 165, 0);
    } else if (randomer == 20) {
        return Color(255, 165, 0);
    } else if (randomer == 21) {
        return Color(255, 0, 0);
    } else if (randomer == 22) {
        return Color(255, 0, 0);
    } else if (randomer == 23) {
        return Color(255, 0, 0);
    } else if (randomer == 24) {
        return Color(255, 0, 0);
    } else if (randomer == 25) {
        return Color(255, 0, 0);
    } else if (randomer == 26) {
        return Color(255, 0, 0);
    } else if (randomer == 27) {
        return Color(255, 0, 0);
    } else if (randomer == 28) {
        return Color(255, 0, 0);
    } else if (randomer == 29) {
        return Color(255, 165, 0);
    } else if (randomer == 30) {
        return Color(255, 165, 0);
    } else if (randomer == 31) {
        return Color(255, 165, 0);
    } else if (randomer == 32) {
        return Color(255, 165, 0);
    } else if (randomer == 33) {
        return Color(255, 165, 0);
    } else if (randomer == 34) {
        return Color(255, 165, 0);
    } else if (randomer == 35) {
        return Color(255, 165, 0);
    } else if (randomer == 36) {
        return Color(255, 165, 0);
    } else if (randomer == 37) {
        return Color(255, 165, 0);
    } else if (randomer == 38) {
        return Color(255, 165, 0);
    } else if (randomer ==39) {
        return Color(255, 165, 0);
    } else if (randomer == 40) {
        return Color(255, 165, 0);
    } else if (randomer == 41) {
        return Color(255, 165, 0);
    } else if (randomer == 42) {
        return Color(255, 165, 0);
    }else if (randomer == 43) {
        return Color(255, 165, 0);
    }else if (randomer == 44) {
        return Color(255, 165, 0);
    }else if (randomer == 45) {
        return Color(255, 165, 0);
    }else if (randomer == 46) {
        return Color(255, 0, 0);
    }else if (randomer == 47) {
        return Color(255, 0, 0);
    }else if (randomer == 48) {
        return Color(255, 0, 0);
    }else if (randomer == 49) {
        return Color(255, 0, 0);
    }else if (randomer == 50) {
        return Color(255, 0, 0);
    }else if (randomer == 51) {
        return Color(255, 0, 0);
    }else if (randomer == 52) {
        return Color(255, 0, 0);
    }else if (randomer == 53) {
        return Color(255, 0, 0);
    }else if (randomer == 54) {
        return Color(255, 0, 0);
    }else if (randomer == 55) {
        return Color(255, 0, 0);
    }else if (randomer == 56) {
        return Color(255, 0, 0);

    }else if (randomer == 57) {
        return Color(255, 0, 0);
    }else if (randomer == 58) {
        return Color(255, 0, 0);
    }else if (randomer == 59) {
        return Color(255, 0, 0);
    }else if (randomer == 60) {
        return Color(255, 0, 0);
    }
    else {
        randomer = 0;
        return Color(255, 0, 0);
    }
}

class Rect {
    public:
	float x;
    float y;
    float width;
    float height;
	
    Rect() {
        this -> x = 0;
        this -> y = 0;
        this -> width = 0;
        this -> height = 0;
    }
	
    Rect(float x, float y, float width, float height) {
        this -> x = x;
        this -> y = y;
        this -> width = width;
        this -> height = height;
    }
	
    bool operator == (const Rect & src) const {
        return (src.x == this -> x && src.y == this -> y && src.height == this -> height && src.width == this -> width);
    }
	
    bool operator != (const Rect & src) const {
        return (src.x != this -> x && src.y != this -> y && src.height != this -> height && src.width != this -> width);
    }
};

struct Vec3 {
    float x, y, z;
};

struct Vec4 {
    float X;
    float Y;
    float Z;
    float W;
    
    Vec4() {
        this->X = 0;
        this->Y = 0;
        this->Z = 0;
        this->W = 0;
    }
    
    Vec4(float x, float y, float z, float w) {
        this->X = x;
        this->Y = y;
        this->Z = z;
        this->W = w;
 }
};

class Vec2 {
    public:
	float x;
    float y;
	
    Vec2() {
        this -> x = 0;
        this -> y = 0;
    }
	
    Vec2(float x, float y) {
        this -> x = x;
        this -> y = y;
    }
	
    static Vec2 Zero() {
        return Vec2(0.0f, 0.0f);
    }
	
    bool operator != (const Vec2 & src) const {
        return (src.x != x) || (src.y != y);
    }
	
    Vec2 & operator += (const Vec2 & v) {
        x += v.x;
        y += v.y;
        return * this;
    }
	
    Vec2 & operator -= (const Vec2 & v) {
        x -= v.x;
        y -= v.y;
        return * this;
    }
};

struct PlayerBone {
    bool isBone = false;
    Vec2 head;
    Vec2 neck;
    Vec2 cheast;
    Vec2 pelvis;
    Vec2 lSh;
    Vec2 rSh;
    Vec2 lElb;
    Vec2 rElb;
    Vec2 lWr;
    Vec2 rWr;
    Vec2 lTh;
    Vec2 rTh;
    Vec2 lKn;
    Vec2 rKn;
    Vec2 lAn;
    Vec2 rAn;
    Vec2 root;
};

struct PlayerWeapon {
    bool isWeapon = false;
    int id;
    int ammo;
    int maxammo;
};

enum Mode {
    InitMode = 1,
    ESPMode = 2,
    HackMode = 3,
    StopMode = 4,
};

struct Options {
    int aimbotmode;
    int openState;
    bool tracingStatus;
    int priority;
    bool pour;
    int aimingRange;
    int wideView;
    bool isMetroMode;
    bool isRadar;
};

struct Memory {
    bool LessRecoil;
    bool ZeroRecoil;
    bool InstantHit;
    bool SmallCrosshair;
    bool FastParachute;
    bool NoShake;
    bool HitEffect;
};

struct Request {
    int Mode;
    Options options;
    Memory memory;
    int ScreenWidth;
    int ScreenHeight;
};

struct VehicleData {
    char VehicleName[50];
    float Distance;
    float Fuel;
    float Health;
    Vec2 Location;
};

struct ItemData {
    char ItemName[50];
    float Distance;
    Vec2 Location;
};

struct GrenadeData {
    int type;
    float Distance;
    Vec2 Location;
};

struct BoxItemData {
    int ItemType;
    int ItemID[40];
    int ItemCount;
    int Count[40];
    Vec2 Location;
};

struct PlayerData {
    char PlayerNameByte[100];
    int TeamID;
    float Health;
    float Distance;
    bool isBot;
    bool isKnocked;
    Vec4 Precise;
    Vec2 Location;
    Vec2 RadarLocation;
    PlayerWeapon Weapon;
    PlayerBone Bone;
};

struct Response {
    bool Success;
	bool Identified;
    int PlayerCount;
    int VehicleCount;
    int ItemsCount;
    int BoxItemsCount;
    int GrenadeCount;
    PlayerData Players[maxplayerCount];
    VehicleData Vehicles[maxvehicleCount];
    ItemData Items[maxitemsCount];
    BoxItemData BoxItems[maxboxitemsCount];
    GrenadeData Grenade[maxgrenadeCount];
};
#endif
