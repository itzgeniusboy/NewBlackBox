#ifndef SANDHOOK_ELFIMG_H
#define SANDHOOK_ELFIMG_H

#include <string>
#include <elf.h>

namespace SandHook {

class ElfImg {
public:
    ElfImg(const char *elf_path);
    ~ElfImg();

    void* getSymbAddress(const char* symbol_name);

private:
    bool loadElf();
    void* findSymbol(const char* symbol_name);

    std::string elfPath;
    void* baseAddress;
    size_t fileSize;
    Elf64_Ehdr* elfHeader;
    Elf64_Shdr* sectionHeaders;
    char* sectionStringTable;
};

} // namespace SandHook

#endif // SANDHOOK_ELFIMG_H
