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

#include <cstdlib>
#include <arpa/inet.h>
#include "XtrConstants.h" 

#ifndef _XTR_OPTION_H
#define _XTR_OPTION_H

namespace xtr {
class Option
{
public:

    /** Create a new Option from bytes.
     *  This is a factory method that returns Options of different derived
     *  types, if they are known.
     *  @param b buffer to read the option from
     *  @param size INPUT: maximum safe size to read from b <br>
     *              OUTPUT: number of bytes read from b OR 0 in case of an
     *              error parsing the option.
     *  @return a pointer to a Option subclass. <br>
     *          If the type is unknown to the implementation, but the option
     *           is valid according to the spec, an instance of
     *           OptionAny is returned <br>
     *          If the option is not valid (not according to the general spec
     *           and/or not according to a specific type spec), an instance of
     *           OptionNop is returned, AND size is set to 0.
     */
    static Option* createFromBytes(const u_int8_t *b, size_t *size);
    
    Option() {};
    virtual ~Option() {};


    /** Returns a pointer to a newly allocated Option, of the same subtype
     *  as the cloned object */
    virtual Option* clone() const = 0;
   
    /** Returns the type of this option */
    virtual u_int8_t getType() const = 0;

    /** Returns the length of the payload for this option */
    virtual u_int8_t getLength() const = 0;

    /** Returns the size of the options when packed */
    virtual u_int8_t getSize() const = 0;

    /** Writes the binary packed form of this option to the destination
     *  @param dest buffer to write option to
     *  @param size INPUT: maximum safe size to write to dest<br>
     *              OUTPUT: actual number of bytes written
     *  @return XTR_SUCCESS if enough space, XTR_FAIL otherwise
     */
    virtual xtr_result pack(u_int8_t *dest, size_t *size) const = 0;

    /** Two Options are equal if their packed representation are the same */
    bool isEqual(const Option& other) const;

    enum {
        NOP = 0,
        CHAIN_ID = 0xC1,
        LAMPORT  = 0xC0, 
        SEVERITY = 0xCE,
    };
};

    
class OptionNop : public Option
{
public:
    OptionNop() {} ;
    OptionNop(const u_int8_t *b, size_t *size);

    /* @override */
    OptionNop* clone() const { return new OptionNop(*this); }

    /* @override */
    u_int8_t getType() const {return Option::NOP; }
    /* @override */
    u_int8_t getLength() const {return 0;}
    /* @override */
    u_int8_t getSize() const {return 1;}

    /* @override */
    xtr_result pack(u_int8_t *dest, size_t *size) const;
};


/** This class can represent any option, and is used when we
 *  don't have a specific class for an option.
 *  A Nop option is created by the default constructor */

class OptionAny: public Option
{
public:
    OptionAny() : type(Option::NOP), length(0) {};

    OptionAny(const u_int8_t *b, size_t *size);

    OptionAny(u_int8_t type, u_int8_t length, const u_int8_t *p);

    xtr_result initFromBytes(const u_int8_t *b, size_t *size);

    /* @override */
    OptionAny* clone() const { return new OptionAny(*this); }

    /* @override */
    u_int8_t getType() const {return type; }
    /* @override */
    u_int8_t getLength() const {return length;}
    /* @override */
    u_int8_t getSize() const {return (getType() == Option::NOP)?1:getLength()+2; }

    /* @override */
    xtr_result pack(u_int8_t *dest, size_t *size) const;

    xtr_result setType(u_int8_t _type);
    xtr_result setLength(u_int8_t _length);

    u_int8_t *getPayload() {return payload;}

private:
    u_int8_t type;
    u_int8_t length;
    u_int8_t payload[253];
};

#if 0
/** ChainIds are 0,1,2,3, or 4 bytes long. 
 *  A ChainId of length 0 will not be serialized to any bytes */
class XtrChainId : public Id<XTR_MAX_CHAIN_ID_LEN>
{
public:
    XtrChainId(size_t len = 0) : Id<XTR_MAX_CHAIN_ID_LEN>(len) {};
    XtrChainId(const u_int8_t* from, size_t len)
        : Id<XTR_MAX_CHAIN_ID_LEN>(from, len);
    /* @override */
    bool isEqual(const XtrChainId& other) const;
protected:
    /* @override */
    bool isValidLength(size_t len) const {return (len < XTR_MAX_CHAIN_ID_LEN);}
};
#endif

class OptionChainId : public Option
{
public:
    /** Creates an empty chainId option */
    OptionChainId() : id(0) {};

    /** Creates a ChainId option from the array. If the type
     *  is wrong, size is set to 0 upon return, and the option
     *  will serialize to nothing */
    OptionChainId(const u_int8_t *b, size_t *size);

    OptionChainId(u_int16_t _id) {id = _id;}

    /* @override */
    OptionChainId* clone() const { return new OptionChainId(*this); }

    /* @override */
    u_int8_t getType() const {return Option::CHAIN_ID; }
    /* @override */
    u_int8_t getLength() const {return 2; }
    /* @override */
    u_int8_t getSize() const {return 4; }

    u_int16_t getId() const {return id;}
    void setId(u_int16_t _id) {id = _id;}

    /* @override */
    xtr_result pack(u_int8_t *dest, size_t *size) const;
    ~OptionChainId() {};
private:
    u_int16_t id;
};

class OptionSeverity : public Option
{
public:
    /** The Severity levels. These are the same values and names as defined in the
     *  syslog RFC (RCF 3164). The values _ALL, _NONE, and _ABSENT are not valid
     *  to be carried in the X-Trace metadata, and are only defined in this 
     *  implementation for internal use.
     */
    typedef enum {
          EMERG = 0,    //Most severe. Will be logged unless the threshold is _NONE
          ALERT = 1,
       CRITICAL = 2,
          ERROR = 3,
        WARNING = 4,
         NOTICE = 5,
           INFO = 6,
          DEBUG = 7,    //Least severe. Will be logged if threshold is DEBUG or _ALL
           _ALL = 8,    //Only valid for the severity threshold, logs all
          _NONE = 255,  //Only valid for the severity threshold, logs none
         _UNSET = 254,  //Only valid for the severity threshold, means threshold unset
        DEFAULT = NOTICE,
    } Level;

    OptionSeverity() : severity(DEFAULT) {};
    OptionSeverity(const u_int8_t *b, size_t *size);
    OptionSeverity(u_int8_t s) : severity(s & 0x7) {};

    /* @override */
    OptionSeverity* clone() const { return new OptionSeverity(*this); }

    /* @override */
    u_int8_t getType() const {return Option::SEVERITY; }
    /* @override */
    u_int8_t getLength() const {return 1;}
    /* @override */
    u_int8_t getSize() const {return 3; }

    u_int8_t getSeverity() const {return severity;}
    void setSeverity(u_int8_t s) {severity = (s & 0x7);}

    /* @override */
    xtr_result pack(u_int8_t *dest, size_t *size) const;
private:
    u_int8_t severity;
};

/* TODO:
    class XtrOptionDestOpenDHT : public Option
    class XtrOptionDestTCPv4 : public Option
    class XtrOptionDestTCPv6 : public Option
    class XtrOptionDestUDPv4 : public Option
    class XtrOptionDestUDPv6 : public Option
    class XtrOptionDestI3 : public Option
*/

}; //namespace xtr

#endif //_XTR_OPTION_H
