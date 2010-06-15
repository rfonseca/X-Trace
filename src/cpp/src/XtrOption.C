/*
* Copyright (c) 2005,2006,2007 The Regents of the University of California.
* All rights reserved.
* 
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#include <cstring>
#include <cassert>
#include "XtrOption.h"

namespace xtr {
/*********************************************************/

Option*
Option::createFromBytes(const u_int8_t *b, size_t *size)
{
    const u_int8_t *p = b;

    u_int8_t type;
    size_t s;
    Option *newOpt;

    //programming errors
    assert(b);
    assert(size);
    
    if (*size == 0) {
        return new OptionNop();
    }

    type = *p; 
    s = *size;
    switch(type) {
    case NOP:
        newOpt = new OptionNop(b, &s);
        break;
    case CHAIN_ID:
        newOpt = new OptionChainId(b, &s);
        break;
    case SEVERITY:
        newOpt = new OptionSeverity(b, &s);
        break;
    default:
        newOpt = new OptionAny(b, &s);
    }
    if (type != NOP && s == 0) {
        delete newOpt;
        newOpt = new OptionNop();
    }
    *size = s;
    return newOpt;
}

bool
Option::isEqual(const Option& other) const 
{
    size_t s;
    u_int8_t here[XTR_MD_MAX_LENGTH];
    u_int8_t there[XTR_MD_MAX_LENGTH];

    if (getType() != other.getType() ||
        getLength() != other.getLength() ||
        getSize() != other.getSize())
        return 0;

    if (getType() == NOP)
        return 1;

    s = getSize();
    if (pack(here, &s) != XTR_SUCCESS)
        return 0;
    s = getSize();
    if (other.pack(there, &s) != XTR_SUCCESS)
        return 0;
    
    if (memcmp(here, there, s) != 0)
        return 0;
    return 1;
}

/***********************************/
/*         OptionNop            */
/***********************************/

OptionNop::OptionNop(const u_int8_t *b, size_t *size)
{
    assert(b);
    assert(size);

    if (*size == 0)
        return;
    if (*b == Option::NOP)
        *size = 1;
    else 
        *size = 0;
    return;
}

xtr_result
OptionNop::pack(u_int8_t *dest, size_t *size) const
{
    assert(dest);
    assert(size);
    
    if (*size < getSize())
        return XTR_FAIL;
    *dest = getType();
    *size = getSize();
    return XTR_SUCCESS;
}

/***********************************/
/*         OptionAny            */
/***********************************/

OptionAny::OptionAny(u_int8_t _type, u_int8_t _length, const u_int8_t *p)
    : type(_type), length(_length)
{
    if (type == 0) {
        length = 0;
    } else {
        if (length > 0) {
            assert(p);
            if (length <= 253)
                memcpy(payload, p, length);
            else {
                type = 0;
                length = 0;
            }
        }
    }
}

/** Constructor to create an option from an array of bytes. 
 *  This is the "createFromBytes" method. Returns 0 in size if
 *  an error occurred.
 * 
 * @param b byte array to read from
 * @param size Input: available size in b <br>
 *             Output: number of bytes read from b, 0 if an error.
 */
OptionAny::OptionAny(const u_int8_t *b, size_t *size) 
    : type(Option::NOP), length(0)
{
    initFromBytes(b, size);
}

xtr_result
OptionAny::initFromBytes(const u_int8_t *b, size_t *size) 
{
    size_t remain;
    const u_int8_t *p = b;

    //programming errors
    assert(b);
    assert(size);
    
    type = Option::NOP;
    length = 0;

    if (*size == 0) {
        return XTR_FAIL;
    }
    remain = *size;

    type = *p; remain--; p++; 
    if (type == Option::NOP) {
        *size -= remain;
        return XTR_SUCCESS;
    }
    if (remain < 1) {
        //error: type != 0, but available length too small
        type = Option::NOP;
        *size = 0;
        return XTR_FAIL;
    }
    length = *p; remain--; p++;
    if (remain < length) {
        type = Option::NOP;
        length = 0;
        *size = 0;
        return XTR_FAIL;
    }
    if (length) {
        if (length <= 253) {
            memcpy(payload, p, length);
            remain-=length;
            *size -= remain;
        } else {
            type = Option::NOP;
            length = 0;
            *size = 0;
            return XTR_FAIL;
        }
    }
    return XTR_SUCCESS;
}

xtr_result
OptionAny::pack(u_int8_t *dest, size_t *size) const
{
    u_int8_t *p = dest;
    size_t s;
    assert(dest);
    assert(size);
     
    /* determine size */
    s = getSize();

    if (*size < s) {
        return XTR_FAIL;
    } 
    *p = type; p++; 
    if (type != 0) {
        *p = length; p++;
        if (length) {
            memcpy(p, payload, length);
            p += length;
        }
    }
    *size = p - dest;
    return XTR_SUCCESS; 
}

xtr_result 
OptionAny::setType(u_int8_t _type) 
{
    type = _type;
    if (type == Option::NOP)
        length = 0;
    return XTR_SUCCESS;
}

xtr_result
OptionAny::setLength(u_int8_t _length)
{
    if (type == Option::NOP && _length) {
       return XTR_FAIL; 
    }
    length = _length;
    return XTR_SUCCESS;
}

#if 0
/** Compares whether two ids are the same. Two ids are the same if they
 *  are numerically the same. Ids are stored in little-endian order, with
 *  the least significant byte first. 
 */
bool
XtrChainId::isEqual(XtrChainId& other) const
{
    unsigned int i;
    bool equal = 1;
    int lcmp;
    unsigned int l;
    lcmp = length - other.length;
    l = (cmp <= 0)?length:other.length;
    //compare the common part
    for (i = 0; i < l && equal; i++) 
        equal &= (id[i] == other.id[i]);
    if (lcmp > 0) //this is longer
        for (i = other.length; i < length && equal; i++)
            equal &= (id[i] == 0);
    else //other is longer
        for (i = length; i < other.length && equal; i++) 
            equal &= (other.id[i] == 0);
    return equal;
}
#endif

/***********************************/
/*         OptionChainId        */
/***********************************/

OptionChainId::OptionChainId(const u_int8_t *b, size_t *size)
    : id(0)
{
    const u_int8_t *p = b;
    assert(b);
    assert(size);

    if (*size < 4) {
        *size = 0;
        return;
    }
    //type
    if (*p != Option::CHAIN_ID) {
        *size = 0;
        return;
    }
    p++;
    //length
    if (*p != 2) {
        *size = 0;
        return;
    }
    p++;
    //id, in network byte order 
    id = ntohs(*(u_int16_t*)p);
    p += 2;

    assert(p - b == 4);
    *size = 4;
    return;
}

xtr_result
OptionChainId::pack(u_int8_t *dest, size_t *size) const
{
    u_int8_t *p = dest;
    size_t s;
    u_int16_t *pp;
    assert(dest);
    assert(size);
    
    /* determine size */
    s = getSize();

    if (*size < s) {
        return XTR_FAIL;
    } 
    *p = getType(); p++; 
    *p = getLength(); p++;
    pp = (u_int16_t*)p;
    *pp = htons(id);
    p += 2;
    *size = p - dest;
    return XTR_SUCCESS; 
}

/***********************************/
/*         OptionSeverity       */
/***********************************/

OptionSeverity::OptionSeverity(const u_int8_t *b, size_t *size)
    : severity(_DEFAULT)
{
    const u_int8_t *p = b;
    assert(b);
    assert(size);

    if (*size < 3) {
        *size = 0;
        return;
    }
    //type
    if (*p != Option::SEVERITY) {
        *size = 0;
        return;
    }
    p++;
    //length
    if (*p != 1) {
        *size = 0;
        return;
    }
    p++;
    //severity
    severity = (*p & 0x7);
    p++;

    assert(p - b == 3);
    *size = 3;
    return;
}

xtr_result
OptionSeverity::pack(u_int8_t *dest, size_t *size) const
{
    u_int8_t *p = dest;
    size_t s;
    assert(dest);
    assert(size);
    
    /* determine size */
    s = getSize();

    if (*size < s) {
        return XTR_FAIL;
    } 
    *p = getType(); p++; 
    *p = getLength(); p++;
    *p = severity; p++;
    *size = p - dest;
    return XTR_SUCCESS; 
}

}; //namespace xtr
