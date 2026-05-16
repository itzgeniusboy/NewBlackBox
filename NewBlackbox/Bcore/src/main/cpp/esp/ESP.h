#ifndef PAKC_ESP_H
#define PAKC_ESP_H
#include "struct.h"


class ESP
{
  private:
	JNIEnv * _env;
	jobject _cvsView;
	jobject _cvs;


  public:
	  ESP()
	{
		_env = nullptr;
		_cvsView = nullptr;
		_cvs = nullptr;
	}

	ESP(JNIEnv * env, jobject cvsView, jobject cvs)
	{
		this->_env = env;
		this->_cvsView = cvsView;
		this->_cvs = cvs;
	}

	bool isValid() const
	{
		return (_env != nullptr && _cvsView != nullptr && _cvs != nullptr);
	}

	int getWidth() const
	{
		if (isValid())
		{
			jclass canvas = _env->GetObjectClass(_cvs);
			jmethodID width = _env->GetMethodID(canvas, "getWidth", "()I");
			return _env->CallIntMethod(_cvs, width);
		}
		return 0;
	}

	int getHeight() const
	{
		if (isValid())
		{
			jclass canvas = _env->GetObjectClass(_cvs);
			jmethodID width = _env->GetMethodID(canvas, "getHeight", "()I");
			return _env->CallIntMethod(_cvs, width);
		}
		return 0;
	}
	
    void DrawTextjkpapa(Color color, Color outlineColor, const char *txt, Vec2 pos, float size) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawTextMethod = _env->GetMethodID(canvasView, "DrawText",
                                                         "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
            jstring s = _env->NewStringUTF(txt);
            _env->CallVoidMethod(_cvsView, drawTextMethod, _cvs, (int)outlineColor.a, (int)outlineColor.r,
                                 (int)outlineColor.g, (int)outlineColor.b,
                                 s, pos.x, pos.y, size);
            _env->CallVoidMethod(_cvsView, drawTextMethod, _cvs, (int)color.a, (int)color.r,
                                 (int)color.g, (int)color.b,
                                 s, pos.x, pos.y, size);
            _env->DeleteLocalRef(s);
        }
    }
    
	void ResetItemCount()
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, 
											"ResetItemCount", "()V");
			_env->CallVoidMethod(_cvsView, drawtext);
			
		}
	}

	void DrawLine(Color color, float thickness, Vec2 start, Vec2 end)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawline = _env->GetMethodID(canvasView, "DrawLine",
												   "(Landroid/graphics/Canvas;IIIIFFFFF)V");
			_env->CallVoidMethod(_cvsView, drawline, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b,
								 thickness, start.x, start.y, end.x, end.y);
		}
	}
	void DrawRect(Color color, float thickness, Vec2 start, Vec2 end)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawline = _env->GetMethodID(canvasView, "DrawRect",
												   "(Landroid/graphics/Canvas;IIIIFFFFF)V");
			_env->CallVoidMethod(_cvsView, drawline, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b,
								 thickness, start.x, start.y, end.x, end.y);
		}
	}
	
	void DrawRect1(Color color, float thickness, Vec2 start, Vec2 end)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawline = _env->GetMethodID(canvasView, "DrawRect",
												   "(Landroid/graphics/Canvas;IIIIFFFFF)V");
			_env->CallVoidMethod(_cvsView, drawline, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b,
								 thickness, start.x, start.y, end.x, end.y);
		}
	}

	void DrawFilledRect(Color color, Vec2 start, Vec2 end)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawline = _env->GetMethodID(canvasView, "DrawFilledRect",
												   "(Landroid/graphics/Canvas;IIIIFFFF)V");
			_env->CallVoidMethod(_cvsView, drawline, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, start.x, start.y, end.x, end.y);
		}
	}
	
	void DebugText(char *s)
	{
		jclass cls = _env->GetObjectClass(_cvsView);
		jmethodID mid = _env->GetMethodID(cls, "DebugText", "(Ljava/lang/String;)V");
		jstring name = _env->NewStringUTF(s);
		_env->CallVoidMethod(_cvsView, mid, name);
		_env->DeleteLocalRef(name);
	}

	void DrawText(Color color, const char *txt, Vec2 pos, float size)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawText",
												   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
			jstring s = _env->NewStringUTF(txt);
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, s, pos.x, pos.y, size);
			_env->DeleteLocalRef(s);
		}
	}
	
	void DrawTextName(Color color, Vec2 pos, float size, bool isInGame)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawTextName",
												   "(Landroid/graphics/Canvas;IIIIFFFZ)V");
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, pos.x, pos.y, size, isInGame);
		}
	}

	void DrawText1(Color color, const char *txt, Vec2 pos, float size)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawText1",
												   "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
			jstring s = _env->NewStringUTF(txt);
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, s, pos.x, pos.y, size);
			_env->DeleteLocalRef(s);
		}
	}

	void DrawWeapon(Color color, int wid, int ammo, int maxammo, Vec2 pos, float size)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawWeapon",
												   "(Landroid/graphics/Canvas;IIIIIIIFFF)V");
			_env->CallVoidMethod(_cvsView, drawtext, _cvs,(int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, wid, ammo,maxammo, pos.x, pos.y, size);
		}
	}
	
	void DrawDistance(float distance, Vec2 pos, float size)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawDistance",
												   "(Landroid/graphics/Canvas;FFFF)V");
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, distance, pos.x, pos.y, size);
		}
	}
	
	void DrawTriangle(Vec2 pos)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawTriangle",
												   "(Landroid/graphics/Canvas;FF)V");
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, pos.x, pos.y);
		}
	}
	
    
    void DrawName2(Color color, const char *txt,int teamid, Vec2 pos, float size) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawtext = _env->GetMethodID(canvasView, "DrawName",
            "(Landroid/graphics/Canvas;IIIILjava/lang/String;IFFF)V");
            jstring s=_env->NewStringUTF(txt);
            _env->CallVoidMethod(_cvsView, drawtext, _cvs, (int) color.a, (int) color.r,
            (int) color.g, (int) color.b,
             s,teamid, pos.x, pos.y, size);
            _env->DeleteLocalRef(s);
        }
    }
    
    
	void DrawName(const char *txt, int teamid, Vec2 pos)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawName",
												   "(Landroid/graphics/Canvas;Ljava/lang/String;IFF)V");
			jstring s = _env->NewStringUTF(txt);
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, s, teamid, pos.x, pos.y);
			_env->DeleteLocalRef(s);
		}
	}
    

	void DrawEnemyCount(Color color, Vec2 start, Vec2 end) {
        if (isValid()) {
            jclass canvasView = _env->GetObjectClass(_cvsView);
            jmethodID drawline = _env->GetMethodID(canvasView, "DrawEnemyCount",
                                                   "(Landroid/graphics/Canvas;IIIIIIII)V");
            _env->CallVoidMethod(_cvsView, drawline, _cvs, (int) color.a, (int) color.r,
                                 (int) color.g, (int) color.b, (int) start.x, (int) start.y, (int) end.x, (int) end.y);
        }
    }

	void DrawItems(const char *txt, float distance, Vec2 pos)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawItems",
												   "(Landroid/graphics/Canvas;Ljava/lang/String;FFF)V");
			jstring s = _env->NewStringUTF(txt);
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, s, distance, pos.x, pos.y);
			_env->DeleteLocalRef(s);
		}
	}
	
	void DrawListItem(Color color, int itemID, int count, Vec2 pos)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawListItem",
												   "(Landroid/graphics/Canvas;IIIIIIFF)V");
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, (int)color.a, (int)color.r, (int)color.g, (int)color.b, itemID, count, pos.x, pos.y);
		}
	}
	
	void DrawVehicles(const char *txt, float distance, float health, float fuel, Vec2 pos)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawtext = _env->GetMethodID(canvasView, "DrawVehicles",
												   "(Landroid/graphics/Canvas;Ljava/lang/String;FFFFF)V");
			jstring s = _env->NewStringUTF(txt);
			_env->CallVoidMethod(_cvsView, drawtext, _cvs, s, distance, health, fuel, pos.x, pos.y);
			_env->DeleteLocalRef(s);
		}
	}
	
	void DrawFilledCircle(Color color, Vec2 pos, float radius)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawfilledcircle = _env->GetMethodID(canvasView, "DrawFilledCircle",
														   "(Landroid/graphics/Canvas;IIIIFFF)V");
			_env->CallVoidMethod(_cvsView, drawfilledcircle, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, pos.x, pos.y, radius);
		}
	}

	void DrawCircle(Color color, Vec2 pos, float radius, float thickness)
	{
		if (isValid())
		{
			jclass canvasView = _env->GetObjectClass(_cvsView);
			jmethodID drawcircle = _env->GetMethodID(canvasView, "DrawCircle",
													 "(Landroid/graphics/Canvas;IIIIFFFF)V");
			_env->CallVoidMethod(_cvsView, drawcircle, _cvs, (int)color.a, (int)color.r,
								 (int)color.g, (int)color.b, pos.x, pos.y, radius, thickness);
		}
	}
};


#endif // PAKC_ESP_H
