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
#include <cassert>
#include <cstring>

#include "XtrConstants.h"
#include "XtrOption.h"

#ifndef _XTR_METADATA_H
#define _XTR_METADATA_H

namespace xtr {
/*********************************************************/

/** Represents an id as an array of bytes with a maximum length.
 * @param max_len the maximum length of the id in bytes
 */
template <size_t max_len> class Id
{
public:
    Id() : length(0) {}; 
    Id(size_t _length);
    Id(const u_int8_t* from, size_t len); 
    virtual ~Id() {};

    bool isLengthValid() const {return isValidLength(length);};
    size_t getLength() const {return length;};
    const u_int8_t* getBytes() const {return id;};
    u_int8_t& operator[](size_t i) {assert(i < length); return id[i];}; 
    virtual bool isEqual(const Id< max_len>& other) const;

    void setLength(size_t _length) {assert(_length <= max_len); length = _length;};
    xtr_result setBytes(const u_int8_t* from, size_t len);

    xtr_result pack(u_int8_t *dest, size_t *size) const;

    char* toString(char *dest, size_t size) const;
    virtual bool isValidLength(size_t len) const {return (len <= max_len);};
protected:
    size_t length;
    u_int8_t id[max_len];
};


template <size_t max_len>
Id< max_len >::Id(size_t _length) 
    : length(0) 
{
    //assert(_length <= max_len); 
    memset(id, 0, sizeof(id));
    if (isValidLength(_length)) {
        length = _length; 
    }
}

template <size_t max_len>
Id< max_len >::Id(const u_int8_t* from, size_t len)
    : length(0)
{
    assert(from);
    //assert(len <= max_len);
    if (isValidLength(len)) {
        length = len;
        memset(id, 0, sizeof(id));
        memcpy(id, from, len);
    }
};

template <size_t max_len>
xtr_result Id<max_len>::setBytes(const u_int8_t* from, size_t len) {
    assert(from);
    assert(len <= max_len);
    if (isValidLength(len)) {   
        length = len;
        memcpy(id, from, len);
        return XTR_SUCCESS;
    } else
        return XTR_FAIL;
}

template <size_t max_len>
xtr_result Id <max_len>::pack(u_int8_t *dest, size_t *size) const
{
    assert(dest);
    assert(size);
    if (*size < length) {
        *size = 0;
        return XTR_FAIL;
    }
    memcpy(dest, id, length);
    *size = length;
    return XTR_SUCCESS;
}
 
template <size_t max_len>
bool Id< max_len >::isEqual(const Id< max_len>& other) const
{
    unsigned int i;
    bool equal = 1;
    if (length != other.length)
        equal =  0;
    for (i = 0; i < length && equal; i++) {
       equal &= (id[i] == other.id[i]); 
    }
    return equal;
}

void xtr_btoh(u_int8_t b, char*p);

template <size_t max_len>
char* Id< max_len >::toString(char *dest, size_t size) const
{
    char* p = dest;
    size_t k = 0;

    if (!dest)
        return dest;
    *dest = 0;
    if (length == 0)
        return dest;
    if (size < length*2 + 1)
        return dest;

    while ( k < length ) {
	xtr_btoh(id[k++], p);
        p += 2;
    }
    *p = 0;
    return dest;
}

class OpId : public Id<XTR_MAX_OP_ID_LEN>
{
public:
    /** Creates a new OpId with all bytes set to 0.
     *  Default length is 4, and with no parameters this is
     *  the default constructor.
     * @param len The length of the new OpId. Valid lengths 
     *        are 4, 8.
     *        If the len is invalid, the OpId will be created
     *        with length 4 and all 0's.
     */ 
    OpId(size_t len = 4);
    
    /** Creates a new OpId with the contents of from.
     *  @param from byte buffer to create the id from
     *  @param len  the size of the id to be created. The buffer in
     *              from MUST be of at least this size. Valid lengths
     *              are 4 and 8. If the length is invalid,
     *              the created OpId has length 4 and all 0's as
     *              content.
     */
    OpId(const u_int8_t* from, size_t len);

    /** Returns true if len == 4 or len == 8*/
    bool isValidLength(size_t len) const {
    	return (len == 4 || len == 8);
    };
};


class TaskId : public Id<XTR_MAX_TASK_ID_LEN>
{
public:
    /** Creates a new TaskId with all bytes set to 0.
     *  Default length is 4, and with no parameters this is
     *  the default constructor.
     *  @param len The length of the new TaskId. Valid lengths
     *         are 4, 8, 12, and 20.
     *         If len is invalid, the TaskId will be created
     *         with length 4 and all 0's.
     */
    TaskId(size_t len = 4);
    /** Creates a new TaskId with the contents of from.
     *  @param from byte buffer to create the id from
     *  @param len  the size of the id to be created. The buffer in
     *              from MUST be of at least this size. Valid lengths
     *              are 4, 8, 12, and 20. If the length is invalid,
     *              the created TaskId has length 4 and all 0's as
     *              content.
     */
    TaskId(const u_int8_t* from, size_t len);
    
    /** Returns true if the id is not all 0's */
    bool isValid() const;

    /** Returns true if the length is 4, 8, 12, or 20 */
    bool isValidLength(size_t len) const {
        return (len == 4 || len == 8 || len == 12 || len == 20);
    }
};


/*********************************************************/

/* Note: the fields are u_int8_t because they can't be larger
 * than this according to the spec.
 */
class Options {
public:
    Options() : reserved_count(0), count(0), length(0), opts(0){};
    Options(const u_int8_t *from, u_int8_t len);

    Options(const Options&);

    ~Options();
    Options& operator=(const Options& other);

    size_t getCount() const {return count;};
    size_t getLength() const {return length;};
    
    bool isEqual(const Options& other) const;

    /** Reserves the internal storage for c options.
     *  This function is used to avoid repeated allocations when adding
     *  new options.
     *  @param c number of options to reserve space to. c is the total
     *           number of options, and not in addition to what's already
     *           stored.
     *  @return XTR_SUCCESS if successful, XTR_FAIL otherwise.
     */
    xtr_result reserve(u_int8_t c);
    void clear();

    Option& operator[](size_t i) const {assert(i < count); return *opts[i];};

    xtr_result addOption(const Option& option);

    /** Remove one option from the list. The index starts at 0.
     *  @param i index of option to remove, starting at 0. 
     *  @return XTR_SUCCESS if the index is valid, XTR_FAIL otherwise.
     */
    xtr_result removeOptionAt(u_int8_t i);

    /** Write the options in binary packed form to the array pointed to
     *  by dest.
     *  @param dest array to write the packet options to
     *  @param size input: size of writable area<br>
     *              output: total size used.
     *  @return XTR_SUCCESS if successful, XTR_FAIL otherwise
     */
    xtr_result pack(u_int8_t *dest, size_t *size) const;

private:
    u_int8_t reserved_count;
    u_int8_t count;
    u_int8_t length;
    Option **opts;
};


class Metadata 
{
public:
    /** Default constructor, creates a new Metadata with invalid
     *  taskId and 0 opId, and with no options.
     *  If called with no parameters, the taskId is set to 4.
     *  Version is set to XTR_CURRENT_VERSION
     *  @param tlen the length in bytes of the taskId. Valid values are
     *         4, 8, 12, or 20. If an invalid value is passed, an invalid
     *         taskId of length 4 is set instead.
     *  @param olen the length in bytes of the opId. Valid values are
     *         4 and 8. If an invalid value is passed, 4 is used.             
     */
    Metadata(size_t tlen=4, size_t olen=4) : version(XTR_CURRENT_VERSION), 
                             taskId(tlen), opId(olen) , options()
        {initRandom();};

    /** Creates a new Metadata with the taskId set to the parameter,
     *  opId to a random value, and no options.
     *  @param id taskId to use for the metadata
     *  @param opIdLen  the length of the opId
     */
    Metadata(const TaskId &id, size_t opIdLen=4) : version(XTR_CURRENT_VERSION),
                                                taskId(), opId() , options()
        {initRandom(); setTaskId(id); setRandomOpId(opIdLen);};

    /** Creates a new Metadata with the given taskId and opId, and
     *  no options.
     *  @param id taskId to use for the metadata
     *  @param opId opId to use for the metadata
     */
    Metadata(const TaskId &id, const OpId &opId)
        :  version(XTR_CURRENT_VERSION), taskId(), opId(), options() 
        {initRandom(); setTaskId(id); setOpId(opId); };


    /** Creates a new Metadata from the given C-style string 
     *  @param s c-style string buffer
     *  @param len number of characters to read from, not including
     *         the 0 terminator
     *  @return an Metadata * to a newly allocated object. The
     *         metadata will be invalid if there is an error.
     */
    static Metadata createFromString(const char* s, size_t len)
    { 
        return Metadata(CREATE_FROM_STRING, s, len);
    }

    /** Creates a new Metadata from the given packed binary representation.
     *  
     */
    static Metadata createFromBytes(const u_int8_t* b, size_t len)
    { 
        return Metadata(CREATE_FROM_BYTES, b, len);
    }

    /** Creates a new Metadata with the given length for the taskId and
     *  for the opId. Both the taskId and the opId will be set to random
     *  numbers.
     *  If called with no parameters the taskId and opId will be set to
     *  the default of 4 bytes. 
     *  
     *  If there is an error a metadata will be returned as if 
     *  called from the default constructor, i.e., 0'd taskID of length 
     *  4, 0'd opId, no options.
     *  @param taskIdLen the length in bytes of the taskId of the created metadata.
     *  @param opIdLen  the length of the opId
     */
    static Metadata createRandom(size_t taskIdLen = 4, size_t opIdLen = 4) 
    {
        return Metadata(CREATE_RANDOM, taskIdLen, opIdLen);
    }   

    /** Returns how many bytes a Metadata occupies in binary form in the
     *  array pointed to by to. If to does not have a valid metadata or if
     *  len is not sufficient for a valid metadata, then we return 0.
     *  @param to an array of size at least len where to look for a metadata
     *  @param len minimum size of the array pointed to by to
     *  @return the number of bytes occupied by the metadata if valid, or
     *          0 if the metadata is invalid or len is insuficient
     */    
    static size_t sizeInArray(const u_int8_t *to, size_t len); 


    /** Resets the Metadata to an invalid metadata, with an invalid
     *  taskId of length 4, opId set to 0, and no options.
     *  This is the same as is returned by the default constructor.
     *  
     */
    void clear();
    
    /** Whether this metadata is valid. An Metadata is valid if the
     *  taskId is not all zeroes.
     */
    bool isValid() const {return taskId.isValid();};

    /** Compares this and another Metadata for equality. The taskId,
     *  opId, and options have to be the same. In particular, the options
     *  must all be identical and in the same order 
     */
    bool isEqual(const Metadata& other) const;

    /** Returns the taskId of this Metadata */
    const TaskId& getTaskId() const {return taskId;};
     
    /** Returns the opId of this Metadata */
    const OpId& getOpId() const {return opId;};
    Options& getOptions() {return options;};

    /** Sets the taskId of this Metadata.
     *  @param id taskId to be set
     */
    xtr_result setTaskId(const TaskId &id);

    /** Sets the taskId to a random value of the given length.
     *  @param len length of the new taskId. Valid lengths are
     *         4,8,12, or 20. If an invalid length is passed,
     *         the taskId is not altered.
     *  @return XTR_SUCCESS if length is valid, XTR_FAIL otherwise
     */  
    xtr_result setRandomTaskId(size_t len = 4);

    /** Sets the opId of this Metadata. If the version of the metadata
     *  is 0 and the new opId has length > 4, the version is updated to
     *  1.
     * @param id opId to be set
     */
    xtr_result setOpId(const OpId &id);


    /** Sets the opId to a random value of the given length. If the version 
     *  of the metadata is 0 and the new opId has length > 4, the version 
     *  is updated to 1.
     *  @param opIdLen length of the new opId. Valid lengths are
     *         4 or 8. If an invalid length is passed,
     *         the opId is not altered.
     *  @return XTR_SUCCESS if length is valid, XTR_FAIL otherwise
     */  
    xtr_result setRandomOpId(size_t opIdLen = 4);

    xtr_result addOption(const Option& opt) {return options.addOption(opt);};

    /** Setting of some important options. These are shortcuts to actually
     *  creating and adding the options, but come in handy. */

    /* ChainId: Deprecated */
    /** Returns the chainId contained in the first chainId option present
      * in the metadata. If no chainId option is present, returns 0. */
    u_int16_t getChainId() const;
    /** Sets the first chainId option present in the Metadata to id. If 
     *  there is no chainId present, creates a new option */
    xtr_result setChainId(u_int16_t id);
    /** Sets the first chainId option present in the Metadata to a
     *  RANDOM id. This is used to indicate a fork in the computation.*/
    xtr_result newChainId();

    /* Severity Option Convenience Functions */
    /** Returns the severity in the first Severity option present in the
     *  metadata. If no such option is in, returns OptionSeverity::_UNSET */
    u_int8_t getSeverityThreshold() const;

    /** Sets the first severity option present in the Metadata to s. If 
     *  there is no Severity option present, creates a new option 
     *  @param s severity level. Only valid values are from 
     *           OptionSeverity::EMERG to OptionSeverity:DEBUG.
     *  @return XTR_FAIL_SEVERITY if s is invalid
     *          XTR_FAIL if can't add option
     *          XTR_SUCCESS otherwise.
     */
    xtr_result setSeverityThreshold(u_int8_t s);

    /** Removes all severity options from the metadata. After this call
     *  a call to getSeverity MUST return OptionSeverity::_UNSET.
     *  @return XTR_SUCCESS even if no severity option was present 
                (i.e., the call is idempotent)
     */
    xtr_result unsetSeverityThreshold();

    /* Serialization related functions */
    /** Returns the size of the metadata in bytes when packed */
    size_t sizeAsBytes() const;

    /** Converts the metadata in memory to a packed byte
     * representation.
     * @param to pointer to a prealocated array
     * @param len pointer to an integer.<ul>
     *            <li> input: the maximum size of the array
     *            <li> output: the actual size used (equal to the result
     *            of sizeAsBytes()
     * @return xtr_result XTR_FAIL if *len < sizeAsBytes(), or if to
     * or len are NULL 
     */
    xtr_result pack(u_int8_t *to, size_t *len) const;

    /** Returns the size of the metadata when represented as a
     * string, not including the terminating '\0' character.
     * This is consistent with strlen called on a string
     * representation of the metadata. */

    size_t sizeAsString() const {return sizeAsBytes() * 2;};
    /** Converts the metadata to a string in the preallocated
     * buffer.
     * @param to preallocated buffer. If the call is successful, the
     * contents of buf will be the string representation of the
     * metadata. Otherwise (if len is insufficient, < sizeAsString() +
     * 1), buf will have '\0' in its first position.
     * @param len maximum size of the buffer. This must be at least
     * sizeAsString() + 1.
     * @return pointer to 'to'.
     */
    char* toString(char* to, size_t len) const;

private:
    typedef enum {
        CREATE_RANDOM,
        CREATE_FROM_BYTES,
        CREATE_FROM_STRING,
    } create_t;

    /* Used by named constructor createRandom */
    Metadata (create_t t, size_t taskIdLen = 4, size_t opIdLen = 4);
    Metadata (create_t t, const void *from, size_t len);

    u_int8_t version;
    TaskId taskId;
    OpId opId;
    Options options;

    static void initRandom();

};
}; //namespace Xtr

#endif // _XTR_METADATA_H
